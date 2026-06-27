package org.fleet.backend.dto;

public record RiderVerificationRequest(
        String ghanaCardNumber,
        String drivingLicenseNumber,
        String vehiclePlate
) {
}
