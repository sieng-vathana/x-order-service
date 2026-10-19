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

    public BusinessClient(
            RestClient.Builder builder,
            @Value("${services.business.base-url:http://127.0.0.1:8086}") String baseUrl) {
        this.client = OrderServiceRestClient.create(builder, baseUrl, "/api/v1/businesses");
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
    }
}
