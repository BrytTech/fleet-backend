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

    @Value("${aza.api.key}")
    private String apiKey;

    @Value("${aza.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> createCheckoutSession(String orderNumber, String amount, String customerEmail) {
        String url = apiUrl + "/sessions";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amount);
        requestBody.put("currency", "GHS");
        requestBody.put("order_number", orderNumber);
        requestBody.put("customer_email", customerEmail);
        requestBody.put("success_url", "fleet://payment/success");
        requestBody.put("cancel_url", "fleet://payment/cancel");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Aza session: " + e.getMessage());
        }
    }
}