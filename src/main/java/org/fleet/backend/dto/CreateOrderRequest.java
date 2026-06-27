package org.fleet.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.fleet.backend.entity.VehicleType;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull(message = "Pickup store is required")
        Long pickupStoreId,

        @NotNull(message = "Dropoff store is required")
        Long dropoffStoreId,

        @NotBlank(message = "Please provide package description")
        String packageDescription,

        @NotNull(message = "Package weight is required")
        @Positive(message = "Weight must be positive")
        BigDecimal packageWeight,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType
) {
}