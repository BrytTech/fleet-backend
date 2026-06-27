package org.fleet.backend.controller;

import jakarta.validation.Valid;
import org.fleet.backend.dto.LoginRequest;
import org.fleet.backend.dto.RegisterCustomerRequest;
import org.fleet.backend.dto.RegisterRiderRequest;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VehicleType;
import org.fleet.backend.service.AuthService;
import org.fleet.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
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

        User user = authService.registerCustomer(
                request.firstname(),
                request.lastname(),
                request.password(),
                request.email(),
                request.phone()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registered as a customer successfully");
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());

        return ResponseEntity.ok(response);
    }

    //rider registration api
    @PostMapping( "/register/rider")
    public ResponseEntity<?> registerRider(@Valid @RequestBody RegisterRiderRequest request){

        if (authService.existsByEmail(request.email())){
            return ResponseEntity.badRequest().body("Email is already registered");
        }
        if (authService.existsByPhone(request.phone())){
            return ResponseEntity.badRequest().body("Phone is already registered");
        }

        User user = authService.registerRider(
                request.firstname(),
                request.lastname(),
                request.password(),
                request.email(),
                request.phone(),
                VehicleType.valueOf(request.vehicleType())
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "Rider account created. Please complete identity verification in the app.");
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());

        return ResponseEntity.ok(response);
    }

    //Login authentication
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request){
        String token = authService.authenticate(request.email(), request.password());
        User user = userService.findUserByEmail(request.email());

        Map<String, String> response = new HashMap<>();
        response.put("token",  token);
        response.put("message", "Login successful");
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("role", user.getRole().toString());

        return ResponseEntity.ok(response);
    }
}
