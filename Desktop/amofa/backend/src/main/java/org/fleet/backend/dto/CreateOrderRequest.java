package org.fleet.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank(message = "Pickup address is required")
        String pickupAddress,

        @NotBlank(message = "Dropoff address is required")
        String dropoffAddress,

        @NotBlank(message = "Pickup city is required")
        String pickupCity,

        @NotBlank(message = "Dropoff city is required")
        String dropoffCity,

        @NotBlank(message = "Please provide package description")
        String packageDescription,

        @NotNull(message = "Package weight is required")
        @Positive(message = "Weight must be positive")
        BigDecimal packageWeight
) {
}
