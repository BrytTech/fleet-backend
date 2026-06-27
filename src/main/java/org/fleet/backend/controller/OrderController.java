package org.fleet.backend.controller;

import jakarta.validation.Valid;
import org.fleet.backend.dto.CreateOrderRequest;
import org.fleet.backend.dto.QRScanRequest;
import org.fleet.backend.entity.Order;
import org.fleet.backend.entity.VehicleType;
import org.fleet.backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //CUSTOMER ENDPOINTS
    //Create order with stores
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


     //Get my orders (customer)
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<Order>> getMyOrders() {
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    //Get order by ID (customer)
    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderByIdAndCustomer(id));
    }


    //Customer cancels order (only PENDING)
    @PostMapping("/{id}/cancel/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> cancelOrderByCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderByCustomer(id));
    }

    //Customer confirms delivery (releases payment)
    @PostMapping("/{id}/confirm-delivery")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> confirmDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.customerConfirmDelivery(id));
    }

    //RIDER ENDPOINTS
    //Get available orders (PENDING)
    @GetMapping("/available")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<Order>> getAvailableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrders());
    }

    //Rider accepts order
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> acceptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.acceptOrder(id));
    }

    //Rider scans QR at pickup store
    @PostMapping("/scan-pickup-qr")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> scanPickupQR(@RequestBody QRScanRequest request) {
        return ResponseEntity.ok(orderService.riderScanPickupQR(request.orderId()));
    }

    //Rider scans QR at dropoff store
    @PostMapping("/scan-dropoff-qr")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> scanDropoffQR(@RequestBody QRScanRequest request) {
        return ResponseEntity.ok(orderService.riderScanDropoffQR(request.orderId()));
    }

    //Rider cancels order (only ASSIGNED)
    @PostMapping("/{id}/cancel/rider")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> cancelOrderByRider(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderByRider(id));
    }

    //Get my deliveries (rider)
    @GetMapping("/me/deliveries")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<Order>> getMyDeliveries() {
        return ResponseEntity.ok(orderService.getMyDeliveries());
    }
}