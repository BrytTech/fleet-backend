package org.fleet.backend.controller;

import org.fleet.backend.service.OrderService;
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

    public WebhookController(OrderService orderService) {
        this.orderService = orderService;
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