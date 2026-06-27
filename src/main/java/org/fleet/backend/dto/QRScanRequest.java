package org.fleet.backend.dto;

import jakarta.validation.constraints.NotNull;

public record QRScanRequest(
        @NotNull(message = "Order ID is required")
        Long orderId
) {}