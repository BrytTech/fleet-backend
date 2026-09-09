package org.fleet.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AzaPaymentService {

    @Value("${aza.api.key:}")
    private String apiKey;

    @Value("${aza.api.url:}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> createCheckoutSession(String orderNumber, String amount, String customerEmail) {
        // Checked here rather than at startup. An unset payment key should stop a
        // payment, not the whole courier service: riders still need to be
        // dispatched, parcels still need tracking, and B2B partner orders are
        // settled on account and never touch Aza at all.
        if (apiKey == null || apiKey.isBlank() || apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalStateException(
                    "Aza is not configured — set AZA_API_KEY and AZA_API_URL to take payments");
        }

        String url = apiUrl + "/sessions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amount);
        requestBody.put("currency", "GHS");
        requestBody.put("order_number", orderNumber);
        requestBody.put("customer_email", customerEmail);
        requestBody.put("success_url", "fleet://payment/success");
        requestBody.put("cancel_url", "fleet://payment/cancel");

        System.out.println("Aza Request: " + requestBody);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );
            System.out.println("Aza Response: " + response.getBody());

            Map<String, Object> responseBody = response.getBody();

            // Check if success and extract data
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                // Aza returns "checkoutUrl", not "url"
                String checkoutUrl = (String) data.get("checkoutUrl");
                String sessionId = (String) data.get("id");

                // Return in the format your code expects ("url")
                Map<String, Object> result = new HashMap<>();
                result.put("url", checkoutUrl);
                result.put("id", sessionId);

                return result;
            } else {
                throw new RuntimeException("Aza session creation failed: " + responseBody);
            }

        } catch (Exception e) {
            System.err.println("Aza Error: " + e.getMessage());
            throw new RuntimeException("Failed to create Aza session: " + e.getMessage());
        }
    }
}