package org.fleet.backend.controller;

import org.fleet.backend.dto.ApiResponse;
import org.fleet.backend.dto.RiderVerificationRequest;
import org.fleet.backend.entity.RiderProfile;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
import org.fleet.backend.repository.RiderProfileRepository;
import org.fleet.backend.repository.UserRepository;
import org.fleet.backend.service.FileUploadService;
import org.fleet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rider")
public class RiderController {

    private final RiderProfileRepository riderProfileRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final UserService userService;

    public RiderController(RiderProfileRepository riderProfileRepository,
                           UserRepository userRepository, FileUploadService fileUploadService, UserService userService) {
        this.riderProfileRepository = riderProfileRepository;
        this.userRepository = userRepository;
        this.fileUploadService = fileUploadService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getRiderProfile(Authentication authentication) {
        String email = authentication.getName();
        System.out.println("Getting profile for email: " + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RiderProfile riderProfile = riderProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Rider profile not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("vehicleType", riderProfile.getVehicleType() != null ? riderProfile.getVehicleType().toString() : null);
        response.put("vehiclePlate", riderProfile.getVehiclePlate());
        response.put("ghanacardNumber", riderProfile.getGhanaCardNumber());
        response.put("drivingLicenseNumber", riderProfile.getDrivingLicenseNumber());
        response.put("verificationStatus", riderProfile.getVerificationStatus() != null ? riderProfile.getVerificationStatus().toString() : "PENDING");

        return ResponseEntity.ok(response);
    }

    // Verify rider (submit verification documents)
    @PutMapping("/verify")
    public ResponseEntity<?> verifyRider(
            @RequestBody RiderVerificationRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RiderProfile riderProfile = riderProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Rider profile not found"));

        // Update rider profile with verification data
        riderProfile.setGhanaCardNumber(request.ghanaCardNumber());
        riderProfile.setDrivingLicenseNumber(request.drivingLicenseNumber());
        riderProfile.setVehiclePlate(request.vehiclePlate());
        riderProfile.setVerificationStatus(VerificationStatus.PENDING);

        riderProfileRepository.save(riderProfile);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Verification submitted successfully");
        response.put("status", "PENDING");

        return ResponseEntity.ok(response);
    }

    // Upload verification images
    @PostMapping("/verify/upload")
    public ResponseEntity<?> uploadVerificationImages(
            @RequestParam(value = "ghanaCardFront", required = false) MultipartFile ghanaCardFront,
            @RequestParam(value = "ghanaCardBack", required = false) MultipartFile ghanaCardBack,
            @RequestParam(value = "drivingLicenseFront", required = false) MultipartFile drivingLicenseFront,
            @RequestParam(value = "drivingLicenseBack", required = false) MultipartFile drivingLicenseBack,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RiderProfile riderProfile = riderProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Rider profile not found"));

        // Upload images and store URLs
        if (ghanaCardFront != null && !ghanaCardFront.isEmpty()) {
            String url = fileUploadService.uploadFile(ghanaCardFront);
            riderProfile.setGhanaCardFrontImageUrl(url);
        }
        if (ghanaCardBack != null && !ghanaCardBack.isEmpty()) {
            String url = fileUploadService.uploadFile(ghanaCardBack);
            riderProfile.setGhanaCardBackImageUrl(url);
        }
        if (drivingLicenseFront != null && !drivingLicenseFront.isEmpty()) {
            String url = fileUploadService.uploadFile(drivingLicenseFront);
            riderProfile.setDrivingLicenseFrontImageUrl(url);
        }
        if (drivingLicenseBack != null && !drivingLicenseBack.isEmpty()) {
            String url = fileUploadService.uploadFile(drivingLicenseBack);
            riderProfile.setDrivingLicenseBackImageUrl(url);
        }

        riderProfileRepository.save(riderProfile);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Images uploaded successfully");
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/earnings")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<Map<String, Object>> getEarnings() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findUserByEmail(email);
        RiderProfile rider = user.getRiderProfile();

        if (rider == null) {
            throw new RuntimeException("Rider profile not found");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalEarnings", rider.getTotalEarnings());
        response.put("completedDeliveries", rider.getCompletedDeliveries());
        response.put("riderId", rider.getId());

        return ResponseEntity.ok(response);
    }
}