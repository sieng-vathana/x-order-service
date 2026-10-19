package com.x.order.service;

import com.sharedlib.response.ApiResponse;
import com.x.order.dto.BusinessCurrencyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BusinessClient {
    private final RestClient client;
    private final RestClient storeClient;

    public BusinessClient(
            RestClient.Builder builder,
            @Value("${services.business.base-url:http://127.0.0.1:8086}") String baseUrl,
            @Value("${services.store.base-url:http://127.0.0.1:8087}") String storeBaseUrl) {
        this.client = OrderServiceRestClient.create(builder, baseUrl, "/api/v1/businesses");
        this.storeClient = OrderServiceRestClient.create(builder, storeBaseUrl, "/api/v1/stores");
    }

    public BusinessCurrencyResponse getCurrencySettings(Long businessId) {
        ApiResponse<BusinessCurrencyResponse> response = client.get()
                .uri("/{id}", businessId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Business service returned no currency settings");
        }
        return response.getData();
    }

    public void requireApproved(Long businessId) {
        BusinessCurrencyResponse business = getCurrencySettings(businessId);
        if (business.status() == null || business.status() != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Merchant approval is required before accepting orders");
        }
        if (Boolean.TRUE.equals(business.suspended())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, suspendedMessage("This merchant", business.suspensionReason()));
        }
    }

    /** Refuses new orders for a store the platform has suspended. */
    public void requireStoreOpen(Long storeId) {
        if (storeId == null) return;
        ApiResponse<java.util.Map<String, Object>> response = storeClient.get()
                .uri("/{id}", storeId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        java.util.Map<String, Object> store = response == null ? null : response.getData();
        if (store != null && Boolean.TRUE.equals(store.get("suspended"))) {
            Object reason = store.get("suspensionReason");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, suspendedMessage("This store", reason == null ? null : String.valueOf(reason)));
        }
    }

    private static String suspendedMessage(String who, String reason) {
        return who + " is suspended by TinhLuk" + (reason == null || reason.isBlank() ? "." : ": " + reason.trim());
    }
}
