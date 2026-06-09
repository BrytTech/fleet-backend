package org.fleet.backend.controller;

import jakarta.validation.Valid;
import org.fleet.backend.dto.CreateOrderRequest;
import org.fleet.backend.entity.Order;
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

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> creatOrder(@Valid @RequestBody CreateOrderRequest request){

        Order order = orderService.createOrder(
                request.pickupAddress(),
                request.dropoffAddress(),
                request.pickupCity(),
                request.dropoffCity(),
                request.packageDescription(),
                request.packageWeight()
        );

        return ResponseEntity.ok(order);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<Order>> getMyOrders(){
        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id){
        return ResponseEntity.ok(orderService.getOrderByIdAndCustomer(id));
    }

    @PostMapping("/{id}/cancel/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Order> cancelOrderByCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderByCustomer(id));
    }


    //RIDER
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

    @PostMapping("/{id}/cancel/rider")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> cancelOrderByRider(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrderByRider(id));
    }

    @PatchMapping("/{id}/pickup")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> pickupOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.pickupOrder(id));
    }

    @PatchMapping("/{id}/deliver")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Order> deliverOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.deliverOrder(id));
    }

    @GetMapping("/me/deliveries")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<List<Order>> getMyDeliveries() {
        return ResponseEntity.ok(orderService.getMyDeliveries());
    }
}
