package org.fleet.backend.service;

import org.fleet.backend.entity.Role;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
import org.fleet.backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    //Register Customer
    public User registerCustomer(String firstName, String lastName, String password, String email, String phone){
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(encodedPassword);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(Role.CUSTOMER);
        user.setActive(true);

        return userRepository.save(user);
    }

    //Register Rider
    public User registerRider(String firstName,
                              String lastName,
                              String password,
                              String email,
                              String phone,
                              String idCardNumber,
                              String idCardImageUrl,
                              String drivingLicenseNumber,
                              String drivingLicenseImageUrl
    ){
        String encodedPassword = passwordEncoder.encode(password);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(encodedPassword);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(Role.RIDER);
        user.setActive(true);
        user.setIdCardNumber(idCardNumber);
        user.setIdCardImageUrl(idCardImageUrl);
        user.setDrivingLicenseNumber(drivingLicenseNumber);
        user.setDrivingLicenseImageUrl(drivingLicenseImageUrl);
        user.setVerificationStatus(VerificationStatus.PENDING);

        return userRepository.save(user);
    }

    //check if ID number exits
    public boolean existsByIdCardNumber(String idCardNumber){
        return userRepository.existsByIdCardNumber(idCardNumber);
    }

    //check if Driving license number exits
    public boolean existsByDrivingLicenseNumber(String drivingLicenseNumber){
        return userRepository.existsByDrivingLicenseNumber(drivingLicenseNumber);
    }

    //check if email is registered already
    public boolean existsByEmail(String email){
        return userRepository.existsByEmail(email);
    }

    //check if phone is registered already
    public boolean existsByPhone(String phone){
        return userRepository.existsByPhone(phone);
    }

    //Log in Authentication
    public String authenticate(String email, String password){
           Authentication authentication = authenticationManager.authenticate(
                   new UsernamePasswordAuthenticationToken(email, password)
           );

               String role = authentication
                       .getAuthorities()
                       .stream()
                       .map(auth -> auth.getAuthority())
                       .filter(auth -> auth.startsWith("ROLE_"))
                       .findFirst()
                       .orElseThrow(() -> new RuntimeException("Role not found"))
                       .replace("ROLE_", "");

               return jwtService.generateToken(email, role);
    }
}
