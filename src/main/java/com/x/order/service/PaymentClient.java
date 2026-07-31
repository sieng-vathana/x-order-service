package com.x.order.service;

import com.sharedlib.response.ApiResponse;
import com.x.order.dto.PaymentSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class PaymentClient {
    private final RestClient client;

    public PaymentClient(RestClient.Builder builder,
                         @Value("${services.payment.base-url:http://127.0.0.1:8084}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl + "/api/v1/payments").build();
    }

    public List<PaymentSummaryResponse> listForOrder(Long orderId) {
        ApiResponse<List<PaymentSummaryResponse>> response = client.get()
                .uri(uri -> uri.queryParam("orderId", orderId).build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
        return response == null || response.getData() == null ? List.of() : response.getData();
    }
}
