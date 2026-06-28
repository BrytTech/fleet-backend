package org.fleet.backend.controller;

import jakarta.validation.Valid;
import org.fleet.backend.dto.CreateOrderRequest;
import org.fleet.backend.dto.QRScanRequest;
import org.fleet.backend.entity.Order;
import org.fleet.backend.entity.Store;
import org.fleet.backend.entity.User;
import org.fleet.backend.service.OrderService;
import org.fleet.backend.service.StoreService;
import org.fleet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final StoreService storeService;
    private final UserService userService;

    public OrderController(OrderService orderService, StoreService storeService, UserService userService) {
        this.orderService = orderService;
        this.storeService = storeService;
        this.userService = userService;
    }

    @PostMapping("/estimate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> estimatePrice(@Valid @RequestBody CreateOrderRequest request) {
        Store pickupStore = storeService.getStoreById(request.pickupStoreId());
        Store dropoffStore = storeService.getStoreById(request.dropoffStoreId());

        double distance = calculateDistance(
                pickupStore.getLatitude(), pickupStore.getLongitude(),
                dropoffStore.getLatitude(), dropoffStore.getLongitude()
        );

        BigDecimal price = orderService.calculatePrice(
                request.packageWeight(),
                distance,
                request.vehicleType()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("price", price);
        response.put("distance", distance);

        return ResponseEntity.ok(response);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(
                request.pickupStoreId(),
                request.dropoffStoreId(),
                request.packageDescription(),
                request.packageWeight(),
                request.vehicleType()
        );
        return ResponseEntity.ok(order);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<Order>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderByIdAndCustomer(id));
    }

    @PostMapping("/{id}/cancel/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> cancelOrderByCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderByCustomer(id));
    }

    @GetMapping("/rider/{id}")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> getOrderForRider(@PathVariable Long id) {
        String riderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User rider = userService.findUserByEmail(riderEmail);

        Order order = orderService.getOrderById(id);

        if (order.getRider() == null || !order.getRider().getId().equals(rider.getRiderProfile().getId())) {
            throw new RuntimeException("You are not assigned to this order");
        }

        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/confirm-delivery")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> confirmDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.customerConfirmDelivery(id));
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<Order>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrders());
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> acceptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.acceptOrder(id));
    }

    @PostMapping("/scan-pickup-qr")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> scanPickupQR(@RequestBody QRScanRequest request) {
        return ResponseEntity.ok(orderService.riderScanPickupQR(request.orderId()));
    }

    @PostMapping("/scan-dropoff-qr")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> scanDropoffQR(@RequestBody QRScanRequest request) {
        return ResponseEntity.ok(orderService.riderScanDropoffQR(request.orderId()));
    }

    @PostMapping("/{id}/cancel/rider")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> cancelOrderByRider(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderByRider(id));
    }

    @GetMapping("/me/deliveries")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<Order>> getMyDeliveries() {
        return ResponseEntity.ok(orderService.getMyDeliveries());
    }
}