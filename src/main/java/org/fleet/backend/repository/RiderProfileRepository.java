package org.fleet.backend.repository;

import org.fleet.backend.entity.RiderProfile;
import org.fleet.backend.entity.User;
import org.fleet.backend.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiderProfileRepository extends JpaRepository<RiderProfile, Long> {

    Optional<RiderProfile> findByUser_Id(Long userId);

    Optional<RiderProfile> findByUser(User user);

    boolean existsByGhanaCardNumber(String ghanaCardNumber);
    boolean existsByDrivingLicenseNumber(String drivingLicenseNumber);

    List<RiderProfile> findByVerificationStatus(VerificationStatus verificationStatus);
}
