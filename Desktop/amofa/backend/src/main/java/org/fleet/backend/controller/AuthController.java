package org.fleet.backend.controller;

import jakarta.validation.Valid;
import org.fleet.backend.dto.LoginRequest;
import org.fleet.backend.dto.RegisterCustomerRequest;
import org.fleet.backend.dto.RegisterRiderRequest;
import org.fleet.backend.service.AuthService;
import org.fleet.backend.service.FileUploadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final FileUploadService fileUploadService;

    public AuthController(AuthService authService, FileUploadService fileUploadService) {
        this.authService = authService;
        this.fileUploadService = fileUploadService;
    }

    //customer registration api
    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request){

        if (authService.existsByEmail(request.email())){
            return ResponseEntity.badRequest().body("Email is already registered");
        }
        if (authService.existsByPhone(request.phone())){
            return ResponseEntity.badRequest().body("Phone is already Registered");
        }

        authService.registerCustomer(
                request.firstname(),
                request.lastname(),
                request.password(),
                request.email(),
                request.phone()
        );
        return ResponseEntity.ok("Registered as a customer successfully");
    }

    //rider registration api
    @PostMapping(value = "/register/rider", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerRider(@Valid @ModelAttribute RegisterRiderRequest request){

        if (authService.existsByEmail(request.email())){
            return ResponseEntity.badRequest().body("Email is already registered");
        }
        if (authService.existsByPhone(request.phone())){
            return ResponseEntity.badRequest().body("Phone is already registered");
        }
        if (authService.existsByIdCardNumber(request.idCardNumber())){
            return ResponseEntity.badRequest().body("Id Card Number already registered");
        }
        if (authService.existsByDrivingLicenseNumber(request.drivingLicenseNumber())){
            return ResponseEntity.badRequest().body("Driving license number already registered");
        }

        //Upload image and get url
        String idCardImageUrl = fileUploadService.uploadFile(request.idCardImage());
        String drivingLicenseImageUrl = fileUploadService.uploadFile(request.drivingLicenseImage());

        authService.registerRider(
                request.firstname(),
                request.lastname(),
                request.password(),
                request.email(),
                request.phone(),
                request.idCardNumber(),
                idCardImageUrl,
                request.drivingLicenseNumber(),
                drivingLicenseImageUrl
        );
        return ResponseEntity.ok("Registered as a rider successfully.Awaiting admin approval");
    }

    //Login authentication
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request){
        String token = authService.authenticate(request.email(), request.password());

        Map<String, String> response = new HashMap<>();
        response.put("token",  token);
        response.put("message", "Login successful");

        return ResponseEntity.ok(response);
    }
}
