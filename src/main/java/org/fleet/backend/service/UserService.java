package org.fleet.backend.service;

import org.fleet.backend.dto.RiderDocumentResponse;
import org.fleet.backend.entity.RiderProfile;
import org.fleet.backend.entity.Role;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
import org.fleet.backend.repository.RiderProfileRepository;
import org.fleet.backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RiderProfileRepository riderProfileRepository;

    public UserService(UserRepository userRepository, RiderProfileRepository riderProfileRepository) {
        this.userRepository = userRepository;
        this.riderProfileRepository = riderProfileRepository;
    }

    //Log in(find user by email)
    public User findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("user with " + email + " not found"));
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findUserByEmail(email);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        user.getRole().getPermission().forEach(permission -> {
            authorities.add(new SimpleGrantedAuthority(permission.name()));
        });

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }



    // Rider verification methods
    public List<RiderProfile> getRidersByVerificationStatus(VerificationStatus status) {
        return riderProfileRepository.findByVerificationStatus(status);
    }

    public List<User> getAllRiders() {
        return userRepository.findByRole(Role.RIDER);
    }

    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void approveRider(Long riderId) {
        User rider = findUserById(riderId);

        if (rider.getRole() != Role.RIDER) {
            throw new RuntimeException("User is not a rider");
        }

        RiderProfile riderProfile = riderProfileRepository
                .findByUser_Id(riderId)
                        .orElseThrow(() -> new RuntimeException("Rider profile not found"));


        riderProfile.setVerificationStatus(VerificationStatus.VERIFIED);
        riderProfile.setVerifiedAt(LocalDateTime.now());

        riderProfileRepository.save(riderProfile);
    }

    public void rejectRider(Long riderId, String reason) {
        User rider = findUserById(riderId);

        if (rider.getRole() != Role.RIDER) {
            throw new RuntimeException("User is not a rider");
        }

        RiderProfile riderProfile = riderProfileRepository
                .findByUser_Id(riderId)
                        .orElseThrow(() -> new RuntimeException("Rider profile not found"));

        riderProfile.setVerificationStatus(VerificationStatus.REJECTED);
        riderProfile.setRejectionReason(reason);
        riderProfileRepository.save(riderProfile);
    }

    public RiderDocumentResponse getRiderDocuments(Long id) {
        User rider = findUserById(id);
        RiderProfile profile = rider.getRiderProfile();

        if (profile == null) {
            throw new RuntimeException("Rider profile not found");
        }

        return new RiderDocumentResponse(
                profile.getGhanaCardNumber(),
                profile.getGhanaCardFrontImageUrl(),
                profile.getGhanaCardBackImageUrl(),
                profile.getDrivingLicenseNumber(),
                profile.getDrivingLicenseFrontImageUrl(),
                profile.getDrivingLicenseBackImageUrl(),
                profile.getVehiclePlate(),
                profile.getVerificationStatus().name()
        );
    }

    public List<User> getAllCustomers() {
        return userRepository.findByRole(Role.CUSTOMER);
    }
}
