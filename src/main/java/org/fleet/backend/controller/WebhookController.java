package org.fleet.backend.controller;

import jakarta.validation.Valid;
import org.fleet.backend.dto.WebhookEndpointRequest;
import org.fleet.backend.entity.PartnerWebhook;
import org.fleet.backend.repository.PartnerWebhookRepository;
import org.fleet.backend.service.OrderService;
import org.fleet.backend.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final OrderService orderService;
    private final PartnerWebhookRepository webhookRepository;
    private final UserService userService;

    public WebhookController(OrderService orderService,
                             PartnerWebhookRepository webhookRepository,
                             UserService userService) {
        this.orderService = orderService;
        this.webhookRepository = webhookRepository;
        this.userService = userService;
    }

    /**
     * Registers where a B2B partner is called back, and the secret we sign with.
     *
     * <p>Admin-only. The secret is write-once from here and never read back by any
     * endpoint: a partner that loses theirs registers a new one rather than asking
     * us to recite it.
     */
    @PostMapping("/endpoints")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerEndpoint(@Valid @RequestBody WebhookEndpointRequest request) {
        if (!request.url().startsWith("https://") && !request.url().startsWith("http://localhost")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Webhook url must be https (localhost is allowed for development)"));
        }

        PartnerWebhook endpoint = new PartnerWebhook();
        endpoint.setUrl(request.url());
        endpoint.setSecret(request.secret());
        endpoint.setActive(true);
        if (request.partnerUserId() != null) {
            endpoint.setPartner(userService.findUserById(request.partnerUserId()));
        }
        PartnerWebhook saved = webhookRepository.save(endpoint);

        logger.info("Registered partner webhook {} -> {}", saved.getId(), saved.getUrl());
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "url", saved.getUrl(),
                "active", saved.isActive()));
    }

    @PostMapping("/aza")
    public ResponseEntity<?> handleAzaWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("AZA WEBHOOK RECEIVED");
        logger.info("Full payload: {}", payload);

        try {
            // Extract event type
            String event = (String) payload.get("event");
            logger.info("Event type: {}", event);

            // Check if this is a completed payment
            if ("checkout.completed".equals(event)) {
                // Get the sessionId from payload
                String sessionId = (String) payload.get("sessionId");
                logger.info("Session ID: {}", sessionId);

                if (sessionId != null) {
                    // We need to find the order by sessionId
                    // You'll need to store sessionId in your Order entity
                    // Then call markOrderAsPaidBySessionId
                    orderService.markOrderAsPaidBySessionId(sessionId);

                    return ResponseEntity.ok(Map.of(
                            "status", "success",
                            "message", "Order marked as paid",
                            "sessionId", sessionId
                    ));
                } else {
                    logger.warn("⚠️ No sessionId found in webhook payload");
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "Missing sessionId in webhook"
                    ));
                }
            }

            // Acknowledge receipt for other events
            return ResponseEntity.ok(Map.of(
                    "status", "received",
                    "message", "Webhook received but not processed",
                    "event", event
            ));

        } catch (Exception e) {
            logger.error("Error processing webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to process webhook: " + e.getMessage()
                    ));
        }
    }

    // Test endpoint
    @GetMapping("/aza")
    public ResponseEntity<?> testWebhook() {
        return ResponseEntity.ok(Map.of(
                "status", "Webhook endpoint is live",
                "message", "Your webhook is working!",
                "timestamp", System.currentTimeMillis()
        ));
    }
}