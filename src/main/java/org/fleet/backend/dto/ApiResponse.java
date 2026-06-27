package org.fleet.backend.dto;

public record ApiResponse(
        String message,
        boolean success
) {
}
