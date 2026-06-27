package org.fleet.backend.controller;

import org.fleet.backend.dto.ApiResponse;
import org.fleet.backend.dto.RejectRiderRequest;
import org.fleet.backend.dto.RiderDocumentResponse;
import org.fleet.backend.entity.Order;
import org.fleet.backend.entity.RiderProfile;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
import org.fleet.backend.service.OrderService;
import org.fleet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;

    public AdminController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    //Get all riders PENDING verification
    @GetMapping("/riders/pending")
    public ResponseEntity<List<RiderProfile>> getPendingRiders(){
        return ResponseEntity.ok(userService.getRidersByVerificationStatus(VerificationStatus.PENDING));
    }

    //Get all riders PENDING + VERIFIED + REJECTED
    @GetMapping("/riders")
    public ResponseEntity<List<User>> getAllRiders(){
        return ResponseEntity.ok(userService.getAllRiders());
    }

    // Get rider's documents - Pass userId to service
    @GetMapping("/riders/{userId}/documents")
    public ResponseEntity<RiderDocumentResponse> getRiderDocuments(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getRiderDocuments(userId));
    }

    // Approve a rider - uses userId (same as /admin/riders response)
    @PutMapping("/riders/{userId}/approve")
    public ResponseEntity<ApiResponse> approveRider(@PathVariable Long userId) {
        userService.approveRider(userId);
        return ResponseEntity.ok(new ApiResponse("Rider approved", true));
    }

    // Reject a rider with reason - uses userId
    @PutMapping("/riders/{userId}/reject")
    public ResponseEntity<ApiResponse> rejectRider(@PathVariable Long userId,
                                                   @RequestBody RejectRiderRequest request) {
        userService.rejectRider(userId, request.reason());
        return ResponseEntity.ok(new ApiResponse("Rider rejected: " + request.reason(), true));
    }

    // Get all customers
    @GetMapping("/customers")
    public ResponseEntity<List<User>> getAllCustomers() {
        return ResponseEntity.ok(userService.getAllCustomers());
    }

    // Get all orders
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // Get order by ID
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // Update order status
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

}