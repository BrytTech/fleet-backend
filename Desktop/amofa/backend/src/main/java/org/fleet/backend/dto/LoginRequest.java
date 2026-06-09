package org.fleet.backend.dto;

public record LoginRequest(
        String email,
        String password
) {
}
