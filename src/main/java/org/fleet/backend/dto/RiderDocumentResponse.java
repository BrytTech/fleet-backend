package org.fleet.backend.dto;

public record RiderDocumentResponse(
        String ghanaCardNumber,
        String ghanaCardFrontImageUrl,
        String ghanaCardBackImageUrl,
        String drivingLicenseNumber,
        String drivingLicenseFrontImageUrl,
        String drivingLicenseBackImageUrl,
        String vehiclePlate,
        String verificationStatus
) {
}
