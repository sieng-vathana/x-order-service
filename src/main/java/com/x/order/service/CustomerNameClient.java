package com.x.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Reads a customer's display name for merchant-facing order notifications. */
@Component
public class CustomerNameClient {
    private static final Logger log = LoggerFactory.getLogger(CustomerNameClient.class);
    private final RestClient client;

    public CustomerNameClient(
            RestClient.Builder builder,
            @Value("${services.customer.base-url:http://127.0.0.1:8091}") String baseUrl) {
        this.client = OrderServiceRestClient.create(builder, baseUrl, "/api/v1/customers");
    }

    public String findName(Long businessId, Long customerId) {
        if (businessId == null || customerId == null || customerId < 0) {
            return null;
        }
        try {
            JsonNode response = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/{id}")
                            .queryParam("businessId", businessId)
                            .build(customerId))
                    .retrieve()
                    .body(JsonNode.class);
            return cleanName(response == null ? null : response.path("data").path("fullName").asText(null));
        } catch (RestClientException exception) {
            log.warn("Customer name lookup failed; Telegram alert will omit the name");
            return null;
        }
    }

    private static String cleanName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String cleaned = name.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
