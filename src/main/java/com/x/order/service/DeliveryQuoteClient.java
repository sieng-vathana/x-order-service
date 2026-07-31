package com.x.order.service;

import com.sharedlib.response.ApiResponse;
import com.x.order.dto.DeliveryQuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DeliveryQuoteClient {
    private final RestClient client;

    public DeliveryQuoteClient(RestClient.Builder builder,
                               @Value("${services.delivery.base-url:http://127.0.0.1:8090}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl + "/api/v1/deliveries").build();
    }

    public DeliveryQuoteResponse getQuote(Long quoteId) {
        ApiResponse<DeliveryQuoteResponse> response = client.get().uri("/quotes/{id}", quoteId)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Delivery service returned no quote");
        }
        return response.getData();
    }
}
