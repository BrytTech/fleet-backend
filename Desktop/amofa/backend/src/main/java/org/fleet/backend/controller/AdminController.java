package org.fleet.backend.controller;

import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
import org.fleet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    //Get all riders PENDING verification
    @GetMapping("/riders/pending")
    public ResponseEntity<List<User>> getPendingRiders(){
        return ResponseEntity.ok(userService.getRidersByVerificationStatus(VerificationStatus.PENDING));
    }

    //Get all riders PENDING + APPROVED + REJECTED
    @GetMapping("/riders")
    public ResponseEntity<List<User>> getAllRiders(){
        return ResponseEntity.ok(userService.getAllRiders());
    }

    // Get rider's documents
    @GetMapping("/riders/{id}/documents")
    public ResponseEntity<Map<String, String>> getRiderDocuments(@PathVariable Long id) {
        User rider = userService.findUserById(id);

        Map<String, String> documents = new HashMap<>();
        documents.put("idCardNumber", rider.getIdCardNumber());
        documents.put("idCardImageUrl", rider.getIdCardImageUrl());
        documents.put("drivingLicenseNumber", rider.getDrivingLicenseNumber());
        documents.put("drivingLicenseImageUrl", rider.getDrivingLicenseImageUrl());
        documents.put("verificationStatus", rider.getVerificationStatus().name());

        return ResponseEntity.ok(documents);
    }

    // Approve a rider
    @PostMapping("/riders/{id}/approve")
    public ResponseEntity<?> approveRider(@PathVariable Long id) {
        userService.approveRider(id);
        return ResponseEntity.ok("Rider approved successfully");
    }

    // Reject a rider with reason
    @PostMapping("/riders/{id}/reject")
    public ResponseEntity<?> rejectRider(@PathVariable Long id,
                                         @RequestParam String reason) {
        userService.rejectRider(id, reason);
        return ResponseEntity.ok("Rider rejected: " + reason);
    }

}
