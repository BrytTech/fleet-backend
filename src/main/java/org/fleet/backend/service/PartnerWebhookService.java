package org.fleet.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fleet.backend.entity.Order;
import org.fleet.backend.entity.PartnerWebhook;
import org.fleet.backend.repository.PartnerWebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tells B2B partners what happened to their parcels.
 *
 * <p>The signature scheme is {@code t=<unix>,v1=<hex>} in {@code X-Fleet-Signature},
 * where the signed material is {@code "<timestamp>.<raw json body>"} under
 * HMAC-SHA256. The timestamp is inside the signed material on purpose: without
 * it a captured callback could be replayed forever, since the body alone never
 * goes stale.
 *
 * <p>Delivery is best-effort and never breaks the rider's action. A rider marking
 * a parcel delivered must succeed whether or not a partner's server is reachable
 * — their parcel is delivered either way, and the alternative is a courier app
 * that stops working because somebody else's API is down.
 */
@Service
public class PartnerWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PartnerWebhookService.class);

    private final PartnerWebhookRepository webhookRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public PartnerWebhookService(PartnerWebhookRepository webhookRepository, ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes one order event to every registered endpoint.
     *
     * @param event the event name, e.g. {@code order.picked_up}
     */
    public void publish(Order order, String event) {
        // Only partner orders travel outward. An ordinary customer's delivery is
        // nobody else's business, and shipping it to a partner endpoint would leak
        // one customer's name, phone and home address to another company.
        if (order.getExternalOrderId() == null || order.getExternalOrderId().isBlank()) {
            return;
        }

        var endpoints = webhookRepository.findByActiveTrue();
        if (endpoints.isEmpty()) {
            log.debug("No partner webhook registered; dropping {} for {}", event, order.getOrderNumber());
            return;
        }

        String body;
        try {
            body = objectMapper.writeValueAsString(buildPayload(order, event));
        } catch (Exception e) {
            log.error("Could not serialise {} for order {}", event, order.getId(), e);
            return;
        }

        for (PartnerWebhook endpoint : endpoints) {
            send(endpoint, body, event, order);
        }
    }

    private void send(PartnerWebhook endpoint, String body, String event, Order order) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Fleet-Signature", sign(body, timestamp, endpoint.getSecret()));
            headers.set("X-Fleet-Event-Id", UUID.randomUUID().toString());

            restTemplate.postForEntity(endpoint.getUrl(), new HttpEntity<>(body, headers), String.class);
            log.info("Delivered {} for order {} to {}", event, order.getOrderNumber(), endpoint.getUrl());
        } catch (Exception e) {
            // Deliberately swallowed: see the class comment. Worth a retry queue
            // later, but a failure here must never fail the rider's request.
            log.warn("Partner webhook {} for order {} failed: {}", event, order.getOrderNumber(), e.getMessage());
        }
    }

    /** {@code t=<unix>,v1=<hex>} over {@code "<timestamp>.<body>"}. */
    String sign(String body, long timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign the webhook payload", e);
        }
    }

    private Map<String, Object> buildPayload(Order order, String event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", String.valueOf(order.getId()));
        data.put("externalOrderId", order.getExternalOrderId());
        data.put("orderStatus", order.getOrderStatus() == null ? null : order.getOrderStatus().name());
        data.put("price", order.getPrice());
        data.put("currency", "GHS");
        data.put("vehicleType", order.getVehicleType() == null ? null : order.getVehicleType().name());
        data.put("packageDescription", order.getPackageDescription());
        data.put("pickupAddress", address(
                order.getPickupStore() != null ? order.getPickupStore().getAddress() : order.getPickupAddress(),
                order.getPickupStore() != null ? order.getPickupStore().getCity() : order.getPickupCity(),
                order.getPickupStore() != null ? order.getPickupStore().getLatitude() : order.getPickupLatitude(),
                order.getPickupStore() != null ? order.getPickupStore().getLongitude() : order.getPickupLongitude(),
                order.getSenderName(), order.getSenderPhone()));
        data.put("dropoffAddress", address(
                order.getDropoffStore() != null ? order.getDropoffStore().getAddress() : order.getDropoffAddress(),
                order.getDropoffStore() != null ? order.getDropoffStore().getCity() : order.getDropoffCity(),
                order.getDropoffStore() != null ? order.getDropoffStore().getLatitude() : order.getDropoffLatitude(),
                order.getDropoffStore() != null ? order.getDropoffStore().getLongitude() : order.getDropoffLongitude(),
                order.getRecipientName(), order.getRecipientPhone()));

        if (order.getRider() != null) {
            Map<String, Object> rider = new LinkedHashMap<>();
            rider.put("riderId", String.valueOf(order.getRider().getId()));
            if (order.getRider().getUser() != null) {
                rider.put("name", (order.getRider().getUser().getFirstName() + " "
                        + order.getRider().getUser().getLastName()).trim());
                rider.put("maskedPhone", mask(order.getRider().getUser().getPhone()));
            }
            data.put("rider", rider);
        }

        Map<String, Object> timestamps = new LinkedHashMap<>();
        timestamps.put("assignedAt", iso(order.getAssignedAt()));
        timestamps.put("pickedUpAt", iso(order.getPickedUpAt()));
        timestamps.put("deliveredAt", iso(order.getDeliveredAt()));
        data.put("timestamps", timestamps);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("event", event);
        payload.put("timestamp", java.time.Instant.now().toString());
        // Per-order and monotonic, so a partner can discard a retry that arrives
        // after a later event — the whole point of retrying is that ordering is
        // not guaranteed.
        payload.put("seq", order.getWebhookSeq());
        payload.put("data", data);
        return payload;
    }

    private Map<String, Object> address(String line, String city, Double lat, Double lng,
                                        String name, String phone) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("recipientName", name);
        a.put("recipientPhone", phone);
        a.put("addressLine", line);
        a.put("city", city);
        a.put("latitude", lat);
        a.put("longitude", lng);
        return a;
    }

    /** A rider's number reaches a partner's systems; only the last two digits do. */
    private String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return null;
        }
        return "*".repeat(phone.length() - 2) + phone.substring(phone.length() - 2);
    }

    private String iso(java.time.LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }
}
