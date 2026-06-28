package org.fleet.backend.service;

import jakarta.transaction.Transactional;
import org.fleet.backend.entity.*;
import org.fleet.backend.repository.OrderRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final StoreService storeService;
    private final QRCodeService qrCodeService;
    private final PaymentService paymentService;
    private final AzaPaymentService azaPaymentService;

    public OrderService(OrderRepository orderRepository,
                        UserService userService,
                        NotificationService notificationService,
                        StoreService storeService,
                        QRCodeService qrCodeService,
                        PaymentService paymentService,
                        AzaPaymentService azaPaymentService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.storeService = storeService;
        this.qrCodeService = qrCodeService;
        this.paymentService = paymentService;
        this.azaPaymentService = azaPaymentService;
    }

    //CREATE ORDER
    @Transactional
    public Order createOrder(
            Long pickupStoreId,
            Long dropoffStoreId,
            String packageDescription,
            BigDecimal packageWeight,
            VehicleType vehicleType
    ) {
        if (vehicleType == null) {
            throw new IllegalArgumentException("Vehicle type is required");
        }

        // 1. Get logged-in customer
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);

        // 2. Validate stores
        Store pickupStore = storeService.getStoreById(pickupStoreId);
        Store dropoffStore = storeService.getStoreById(dropoffStoreId);

        if (!pickupStore.getIsActive() || !dropoffStore.getIsActive()) {
            throw new RuntimeException("One or both stores are not active");
        }

        if (pickupStoreId.equals(dropoffStoreId)) {
            throw new RuntimeException("Pickup and dropoff stores must be different");
        }

        // 3. Generate order number
        String orderNumber = "ORD-" + UUID.randomUUID();

        // 4. Calculate distance and price
        double distance = calculateDistance(
                pickupStore.getLatitude(), pickupStore.getLongitude(),
                dropoffStore.getLatitude(), dropoffStore.getLongitude()
        );
        BigDecimal price = calculatePrice(packageWeight, distance, vehicleType);

        // 5. Create order
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomer(customer.getCustomerProfile());
        order.setPickupStore(pickupStore);
        order.setDropoffStore(dropoffStore);
        order.setPackageDescription(packageDescription);
        order.setPackageWeight(packageWeight);
        order.setDistance(BigDecimal.valueOf(distance));
        order.setPrice(price);
        order.setVehicleType(vehicleType);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // 6. Save order first to get ID
        Order savedOrder = orderRepository.save(order);

        // 7. Generate QR code
        String qrCode = qrCodeService.generateQRCode(savedOrder);
        savedOrder.setQrCode(qrCode);

        // 8. Save again with QR code
        Order finalOrder = orderRepository.save(savedOrder);

        // 9. Send notification
        notificationService.createNotification(
                customer.getId(),
                "Order Placed!",
                "Your order #" + finalOrder.getOrderNumber() + " has been placed successfully.",
                "ORDER_CREATED",
                finalOrder.getId()
        );

        // 10. Create Aza checkout session
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
            System.err.println("Failed to create Aza session: " + e.getMessage());
            return finalOrder;
        }
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

        if (order.getRider() == null || !order.getRider().getId().equals(rider.getRiderProfile().getId())) {
            throw new RuntimeException("You are not assigned to this order");
        }

        if (order.getOrderStatus() != OrderStatus.ASSIGNED) {
            throw new RuntimeException("Order cannot be picked up. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.PICKED_UP);
        order.setPickedUpAt(LocalDateTime.now());
        order.setRiderPickupScannedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        notificationService.createNotification(
                order.getCustomer().getUser().getId(),
                "Package Picked Up!",
                "Your package for order #" + updatedOrder.getOrderNumber() + " has been picked up.",
                "ORDER_PICKED_UP",
                updatedOrder.getId()
        );

        return updatedOrder;
    }

    //QR SCAN - DROPOFF
    @Transactional
    public Order riderScanDropoffQR(Long orderId) {
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getRider() == null || !order.getRider().getId().equals(rider.getRiderProfile().getId())) {
            throw new RuntimeException("You are not assigned to this order");
        }

        if (order.getOrderStatus() != OrderStatus.PICKED_UP) {
            throw new RuntimeException("Order cannot be delivered. Current status: " + order.getOrderStatus());
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

        return cancelledOrder;
    }

    //RIDER METHODS
    public List<Order> getAvailableOrders() {
        return orderRepository.findByOrderStatus(OrderStatus.PENDING);
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

        return cancelledOrder;
    }
}