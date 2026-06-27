package org.fleet.backend.dto;

import jakarta.validation.constraints.*;
import org.aspectj.weaver.ast.Not;

public record RegisterRiderRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstname,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastname,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone number required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
        String phone,

        @NotNull(message = "Vehicle type is required")
        String vehicleType
) {
}
