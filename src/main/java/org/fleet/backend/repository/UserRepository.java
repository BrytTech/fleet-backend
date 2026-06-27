package org.fleet.backend.repository;

import org.fleet.backend.entity.Role;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    List<User> findByRole (Role role);
}
