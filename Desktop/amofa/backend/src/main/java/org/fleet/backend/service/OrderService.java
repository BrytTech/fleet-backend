package org.fleet.backend.service;

import jakarta.transaction.Transactional;
import org.fleet.backend.entity.*;
import org.fleet.backend.repository.OrderRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;

    public OrderService(OrderRepository orderRepository, UserService userService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    @Transactional
    public Order createOrder(
            String pickupAddress,
            String dropoffAddress,
            String pickupCity,
            String dropoffCity,
            String packageDescription,
            BigDecimal packageWeight
    ) {
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        User customer = userService.findUserByEmail(customerEmail);

        String orderNumber = "ORD-" + UUID.randomUUID();

        BigDecimal price = calculatePrice(packageWeight, pickupCity, dropoffCity);

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomer(customer);
        order.setPickupAddress(pickupAddress);
        order.setDropoffAddress(dropoffAddress);
        order.setPickupCity(pickupCity);
        order.setDropoffCity(dropoffCity);
        order.setPackageDescription(packageDescription);
        order.setPackageWeight(packageWeight);
        order.setPrice(price);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        return orderRepository.save(order);

    }

    public BigDecimal calculatePrice(BigDecimal packageWeight, String pickupCity, String dropoffCity) {
        double basePrice = 10.0;
        double weightCharge = packageWeight.doubleValue() * 2.0;
        double cityCharge = pickupCity.equals(dropoffCity) ? 5.0 : 20.0;

        return BigDecimal.valueOf(basePrice + weightCharge + cityCharge);
    }

    //Customer
    public List<Order> getMyOrders() {
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);

        return orderRepository.findByCustomer(customer);
    }

    public Order getOrderByIdAndCustomer(Long id) {
        String customerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);

        return orderRepository.findByIdAndCustomer(id, customer)
                .orElseThrow(()-> new RuntimeException("Order not found"));
    }

    public Order cancelOrderByCustomer(Long orderId) {
        String customerEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User customer = userService.findUserByEmail(customerEmail);

        Order order = orderRepository.findByIdAndCustomer(orderId, customer)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Only PENDING orders can be cancelled
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(Role.CUSTOMER);

        return orderRepository.save(order);
    }

    //Rider
    public List<Order> getAvailableOrders(){
        return orderRepository.findByOrderStatus(OrderStatus.PENDING);
    }

    @Transactional
    public Order acceptOrder(Long orderId){
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(()-> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.PENDING ){
            throw new RuntimeException("Order already assigned or delivered");
        }

        order.setRider(rider);
        order.setOrderStatus(OrderStatus.ASSIGNED);
        order.setAssignedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public Order cancelOrderByRider(Long orderId) {
        String riderEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findByIdAndRider(orderId, rider)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Only ASSIGNED orders can be cancelled by rider
        if (order.getOrderStatus() != OrderStatus.ASSIGNED) {
            throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(Role.RIDER);

        return orderRepository.save(order);
    }

    @Transactional
    public Order pickupOrder(Long orderId){
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify this rider is assigned to this order
        if (order.getRider() == null || !order.getRider().getId().equals(rider.getId())) {
            throw new RuntimeException("You are not assigned to this order");
        }

        if (order.getOrderStatus() != OrderStatus.ASSIGNED) {
            throw new RuntimeException("Order cannot be picked up. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.PICKED_UP);
        order.setPickedUpAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Transactional
    public Order deliverOrder(Long orderId) {
        String riderEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify this rider is assigned to this order
        if (order.getRider() == null || !order.getRider().getId().equals(rider.getId())) {
            throw new RuntimeException("You are not assigned to this order");
        }

        if (order.getOrderStatus() != OrderStatus.PICKED_UP) {
            throw new RuntimeException("Order cannot be delivered. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public List<Order> getMyDeliveries() {
        String riderEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        return orderRepository.findByRider(rider);
    }
}