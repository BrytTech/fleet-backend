package org.fleet.backend.service;

import jakarta.transaction.Transactional;
import org.fleet.backend.dto.AddressDto;
import org.fleet.backend.dto.CreateOrderRequest;
import org.fleet.backend.entity.*;
import org.fleet.backend.repository.OrderRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final StoreService storeService;
    private final QRCodeService qrCodeService;
    private final PaymentService paymentService;
    private final AzaPaymentService azaPaymentService;
    private final PartnerWebhookService partnerWebhookService;
    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    public OrderService(OrderRepository orderRepository,
                        UserService userService,
                        NotificationService notificationService,
                        StoreService storeService,
                        QRCodeService qrCodeService,
                        PaymentService paymentService,
                        AzaPaymentService azaPaymentService,
                        PartnerWebhookService partnerWebhookService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.storeService = storeService;
        this.qrCodeService = qrCodeService;
        this.paymentService = paymentService;
        this.azaPaymentService = azaPaymentService;
        this.partnerWebhookService = partnerWebhookService;
    }

    //CREATE ORDER
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        if (request.vehicleType() == null) {
            throw new IllegalArgumentException("Vehicle type is required");
        }

        // 1. Get logged-in customer
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);

        // 2. A repeated external id is a retry, not a second parcel. Returning the
        //    original is what makes a partner's booking call safe to send again
        //    after a timeout, when we cannot know whether the first one landed.
        if (request.externalOrderId() != null && !request.externalOrderId().isBlank()) {
            Optional<Order> existing = orderRepository.findByExternalOrderId(request.externalOrderId());
            if (existing.isPresent()) {
                logger.info("Returning existing order for externalOrderId " + request.externalOrderId());
                return existing.get();
            }
        }

        // 3. Resolve both ends of the journey
        Endpoint pickup = resolveEndpoint(request.pickupStoreId(), request.pickupAddress(), "pickup");
        Endpoint dropoff = resolveEndpoint(request.dropoffStoreId(), request.dropoffAddress(), "dropoff");

        if (pickup.store() != null && dropoff.store() != null
                && pickup.store().getId().equals(dropoff.store().getId())) {
            throw new IllegalArgumentException("Pickup and dropoff stores must be different");
        }

        // 4. Distance and price, always from real coordinates
        double distance = calculateDistance(
                pickup.latitude(), pickup.longitude(),
                dropoff.latitude(), dropoff.longitude());
        BigDecimal price = calculatePrice(request.packageWeight(), distance, request.vehicleType());

        // 5. Create order
        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID());
        order.setCustomer(customer.getCustomerProfile());
        order.setExternalOrderId(
                request.externalOrderId() == null || request.externalOrderId().isBlank()
                        ? null : request.externalOrderId());

        order.setPickupStore(pickup.store());
        order.setPickupAddress(pickup.addressLine());
        order.setPickupCity(pickup.city());
        order.setPickupLatitude(pickup.latitude());
        order.setPickupLongitude(pickup.longitude());

        order.setDropoffStore(dropoff.store());
        order.setDropoffAddress(dropoff.addressLine());
        order.setDropoffCity(dropoff.city());
        order.setDropoffLatitude(dropoff.latitude());
        order.setDropoffLongitude(dropoff.longitude());

        order.setPackageDescription(request.packageDescription());
        order.setPackageWeight(request.packageWeight());
        order.setDistance(BigDecimal.valueOf(distance));
        order.setPrice(price);
        order.setVehicleType(request.vehicleType());
        order.setOrderStatus(OrderStatus.PENDING);

        // The recipient named on the drop-off address wins over the top-level
        // one: it is the more specific statement of who opens the door.
        order.setRecipientName(firstNonBlank(
                dropoff.recipientName(), request.recipientName()));
        order.setRecipientPhone(firstNonBlank(
                dropoff.recipientPhone(), request.recipientPhone()));
        order.setSenderName(firstNonBlank(request.senderName(),
                (customer.getFirstName() + " " + customer.getLastName()).trim()));
        order.setSenderPhone(firstNonBlank(request.senderPhone(), customer.getPhone()));

        if (request.packagePhotos() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                order.setPackagePhotos(mapper.writeValueAsString(request.packagePhotos()));
            } catch (Exception e) {
                order.setPackagePhotos(request.packagePhotos().toString());
            }
        }

        // 6. Save order first to get ID
        Order savedOrder = orderRepository.save(order);

        // 7. Generate QR code
        savedOrder.setQrCode(qrCodeService.generateQRCode(savedOrder));
        Order finalOrder = orderRepository.save(savedOrder);

        // 8. Notify
        notificationService.createNotification(
                customer.getId(),
                "Order Placed!",
                "Your order #" + finalOrder.getOrderNumber() + " has been placed successfully.",
                "ORDER_CREATED",
                finalOrder.getId()
        );

        // 9. Settle it
        if (customer.isPartner()) {
            // Billed on account. Riders are only offered orders that are already
            // paid, so a partner order routed through a hosted checkout page —
            // which no one is sitting in front of — would never reach a rider.
            finalOrder.setPaymentStatus(PaymentStatus.PAID);
            finalOrder.setPaymentReference("ON_ACCOUNT:" + customer.getEmail());
            logger.info("Partner order " + finalOrder.getOrderNumber() + " settled on account");
            Order settled = orderRepository.save(finalOrder);
            publish(settled, "order.created");
            return settled;
        }

        finalOrder.setPaymentStatus(PaymentStatus.PENDING);
        try {
            Map<String, Object> session = azaPaymentService.createCheckoutSession(
                    finalOrder.getOrderNumber(),
                    finalOrder.getPrice().toString(),
                    customer.getEmail()
            );

            finalOrder.setPaymentUrl((String) session.get("url"));
            finalOrder.setPaymentSessionId((String) session.get("id"));

            return orderRepository.save(finalOrder);

        } catch (Exception e) {
            // If Aza fails, order is still created but payment not initiated
            logger.warning("Failed to create Aza session: " + e.getMessage());
            return orderRepository.save(finalOrder);
        }
    }

    /** One end of a journey, however it was given to us. */
    public record Endpoint(
            Store store,
            String addressLine,
            String city,
            double latitude,
            double longitude,
            String recipientName,
            String recipientPhone
    ) {}

    /**
     * Resolves one end of a journey from either a store id or a loose address.
     *
     * <p>Exactly one of the two is required. Accepting neither used to be legal
     * and silently priced the delivery as if it were five kilometres, which
     * charges a real customer a made-up amount; a caller that says nothing about
     * where the parcel goes is a caller with a bug, and it should hear about it.
     */
    public Endpoint resolveEndpoint(Long storeId, AddressDto address, String which) {
        boolean hasStore = storeId != null;
        boolean hasAddress = address != null && address.hasCoordinates();

        if (hasStore && hasAddress) {
            throw new IllegalArgumentException(
                    "Give " + which + " either a store id or an address, not both");
        }
        if (!hasStore && !hasAddress) {
            throw new IllegalArgumentException(
                    "The " + which + " needs a store id, or an address with latitude and longitude");
        }

        if (hasStore) {
            Store store = storeService.getStoreById(storeId);
            if (store.getIsActive() == null || !store.getIsActive()) {
                throw new IllegalArgumentException("The " + which + " store is not active");
            }
            return new Endpoint(store, store.getAddress(), store.getCity(),
                    store.getLatitude(), store.getLongitude(), null, null);
        }

        return new Endpoint(null, address.addressLine(), address.city(),
                address.latitude(), address.longitude(),
                address.recipientName(), address.recipientPhone());
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    /**
     * Bumps the order's event counter and tells any registered partner.
     *
     * <p>The increment is saved before the callback goes out, so a partner that
     * receives two events never sees the same sequence number twice even if the
     * send itself is retried.
     */
    private void publish(Order order, String event) {
        order.setWebhookSeq(order.getWebhookSeq() + 1);
        orderRepository.save(order);
        partnerWebhookService.publish(order, event);
    }

    //CALCULATE DISTANCE
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    //CALCULATE PRICE
    public BigDecimal calculatePrice(BigDecimal packageWeight, double distance, VehicleType vehicleType) {
        double basePrice = 15.0;
        double weightCharge = packageWeight.doubleValue() * 2.0;
        double distanceCharge = distance * 3.0; // 3 GHS per km
        double vehicleMultiplier = vehicleType != null ? vehicleType.getPriceMultiplier() : 1.0;

        return BigDecimal.valueOf((basePrice + weightCharge + distanceCharge) * vehicleMultiplier);
    }

    //MARK ORDER AS PAID (Webhook)
    @Transactional
    public void markOrderAsPaid(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with number: " + orderNumber));

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Payment Successful!",
                "Your payment for order #" + order.getOrderNumber() + " has been confirmed.",
                "PAYMENT_SUCCESS",
                order.getId()
        );

        System.out.println("Order " + orderNumber + " marked as PAID");
    }

    //QR SCAN - PICKUP
    @Transactional
    public Order riderScanPickupQR(Long orderId) {
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getRider() == null) {
            order.setRider(rider.getRiderProfile());
            order.setAssignedAt(LocalDateTime.now());
        }

        if (order.getOrderStatus() == OrderStatus.PICKED_UP || order.getOrderStatus() == OrderStatus.DELIVERED) {
            return order;
        }

        order.setOrderStatus(OrderStatus.PICKED_UP);
        order.setPickedUpAt(LocalDateTime.now());
        order.setRiderPickupScannedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        try {
            notificationService.createNotification(
                    order.getCustomer().getUser().getId(),
                    "Package Picked Up!",
                    "Your package for order #" + updatedOrder.getOrderNumber() + " has been picked up.",
                    "ORDER_PICKED_UP",
                    updatedOrder.getId()
            );
        } catch (Exception e) {
            logger.warning("Notification send failed: " + e.getMessage());
        }

        publish(updatedOrder, "order.picked_up");
        return updatedOrder;
    }

    //QR SCAN - DROPOFF
    @Transactional
    public Order riderScanDropoffQR(Long orderId) {
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getRider() == null) {
            order.setRider(rider.getRiderProfile());
            order.setAssignedAt(LocalDateTime.now());
        }

        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            return order;
        }

        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order.setRiderDropoffScannedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Package Delivered!",
                "Your order #" + updatedOrder.getOrderNumber() + " has been delivered. Please confirm to release payment.",
                "ORDER_DELIVERED",
                updatedOrder.getId()
        );

        publish(updatedOrder, "order.delivered");
        return updatedOrder;
    }

    //CUSTOMER CONFIRMS DELIVERY
    @Transactional
    public Order customerConfirmDelivery(Long orderId) {
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);

        Order order = orderRepository.findByIdAndCustomer(orderId, customer.getCustomerProfile())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Order not yet delivered. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CUSTOMER_CONFIRMED);
        order.setCustomerConfirmedAt(LocalDateTime.now());

        Order confirmedOrder = orderRepository.save(order);

        // RELEASE PAYMENT TO RIDER
        paymentService.releasePaymentToRider(orderId);

        // Notify rider
        notificationService.createNotification(
                order.getRider().getUser().getId(),
                "Delivery Confirmed!",
                "Customer confirmed order #" + confirmedOrder.getOrderNumber() + ". Payment of GHS " + order.getPrice() + " has been released!",
                "PAYMENT_RELEASED",
                confirmedOrder.getId()
        );

        publish(confirmedOrder, "order.customer_confirmed");
        return confirmedOrder;
    }

    //CUSTOMER METHODS
    public List<Order> getMyOrders() {
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);
        return orderRepository.findByCustomerWithDetails(customer.getCustomerProfile());
    }

    public Order getOrderByIdAndCustomer(Long id) {
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);
        return orderRepository.findByIdAndCustomerWithDetails(id, customer.getCustomerProfile())
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order cancelOrderByCustomer(Long orderId) {
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);

        Order order = orderRepository.findByIdAndCustomer(orderId, customer.getCustomerProfile())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(Role.CUSTOMER);

        Order cancelledOrder = orderRepository.save(order);

        notificationService.createNotification(
                customer.getId(),
                "Order Cancelled",
                "Your order #" + cancelledOrder.getOrderNumber() + " has been cancelled.",
                "ORDER_CANCELLED",
                cancelledOrder.getId()
        );

        publish(cancelledOrder, "order.cancelled");
        return cancelledOrder;
    }

    //RIDER METHODS
    public List<Order> getAvailableOrders() {
        return orderRepository.findByOrderStatusAndPaymentStatus(OrderStatus.PENDING, PaymentStatus.PAID);
    }

    @Transactional
    public Order acceptOrder(Long orderId) {
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order already assigned or delivered");
        }

        order.setRider(rider.getRiderProfile());
        order.setOrderStatus(OrderStatus.ASSIGNED);
        order.setAssignedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Rider Assigned!",
                "Rider " + rider.getFirstName() + " " + rider.getLastName() + " has been assigned to your order.",
                "RIDER_ASSIGNED",
                updatedOrder.getId()
        );

        publish(updatedOrder, "order.assigned");
        return updatedOrder;
    }

    public Order cancelOrderByRider(Long orderId) {
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findByIdAndRider(orderId, rider.getRiderProfile())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.ASSIGNED) {
            throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(Role.RIDER);

        Order cancelledOrder = orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Rider Cancelled",
                "The rider has cancelled your order #" + cancelledOrder.getOrderNumber(),
                "RIDER_CANCELLED",
                cancelledOrder.getId()
        );

        publish(cancelledOrder, "order.cancelled");
        return cancelledOrder;
    }

    public List<Order> getMyDeliveries() {
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);
        return orderRepository.findByRider(rider.getRiderProfile());
    }

    //ADMIN METHODS
    public List<Order> getAllOrders() {
        return orderRepository.findAllOrdersWithDetails();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        Order order = getOrderById(id);
        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledBy(Role.ADMIN);
            order.setCancelledAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrderByAdmin(Long orderId) {
        Order order = getOrderById(orderId);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(Role.ADMIN);

        Order cancelledOrder = orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Order Cancelled by Admin",
                "Your order #" + cancelledOrder.getOrderNumber() + " has been cancelled by admin.",
                "ORDER_CANCELLED",
                cancelledOrder.getId()
        );

        publish(cancelledOrder, "order.cancelled");
        return cancelledOrder;
    }

    @Transactional
    public void markOrderAsPaidBySessionId(String sessionId) {
        // Find order by paymentSessionId
        Order order = orderRepository.findByPaymentSessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Order not found with sessionId: " + sessionId));

        // Update payment status
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        // Send notification to customer
        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Payment Successful!",
                "Your payment for order #" + order.getOrderNumber() + " has been confirmed.",
                "PAYMENT_SUCCESS",
                order.getId()
        );

        logger.info("Order " + order.getOrderNumber() + " marked as PAID via webhook");
    }
}