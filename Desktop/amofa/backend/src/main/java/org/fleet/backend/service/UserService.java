package org.fleet.backend.service;

import org.fleet.backend.entity.Role;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
    public List<User> getRidersByVerificationStatus(VerificationStatus status) {
        return userRepository.findByRoleAndVerificationStatus(Role.RIDER, status);
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

        rider.setVerificationStatus(VerificationStatus.APPROVED);
        rider.setVerifiedAt(LocalDateTime.now());
        userRepository.save(rider);
    }

    public void rejectRider(Long riderId, String reason) {
        User rider = findUserById(riderId);

        if (rider.getRole() != Role.RIDER) {
            throw new RuntimeException("User is not a rider");
        }

        rider.setVerificationStatus(VerificationStatus.REJECTED);
        rider.setRejectionReason(reason);
        userRepository.save(rider);
    }
}
