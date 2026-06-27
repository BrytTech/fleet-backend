package org.fleet.backend.controller;

import org.fleet.backend.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final OrderService orderService;

    public WebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/aza")
    public ResponseEntity<?> handleAzaWebhook(@RequestBody Map<String, Object> payload) {
        String event = (String) payload.get("event");

        if ("session.completed".equals(event)) {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String orderNumber = (String) data.get("order_number");

            // Update order payment status
            orderService.markOrderAsPaid(orderNumber);
        }

        return ResponseEntity.ok().build();
    }
}