package org.fleet.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Registers where a partner should be called back, and with what signing secret. */
public record WebhookEndpointRequest(
        @NotBlank(message = "Webhook url is required")
        String url,

        @NotBlank(message = "A signing secret is required")
        String secret,

        /** Optional: the partner account this endpoint belongs to. */
        Long partnerUserId
) {
}
