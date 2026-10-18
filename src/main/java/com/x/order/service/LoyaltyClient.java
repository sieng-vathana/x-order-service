package com.x.order.service;

import com.sharedlib.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/** Loyalty points in the customer service: what points are worth, and settling them when an order completes. */
@Component
public class LoyaltyClient {
    private final RestClient client;

    public LoyaltyClient(RestClient.Builder builder,
                         @Value("${services.customer.base-url:http://127.0.0.1:8091}") String baseUrl) {
        this.client = OrderServiceRestClient.create(builder, baseUrl, "/internal/loyalty");
    }

    /** The discount, in the order currency, that the points are worth. */
    public BigDecimal quote(Long businessId, Long customerId, long points, String currencyCode, BigDecimal usdToKhrRate) {
        record Request(Long businessId, Long customerId, long points, String currencyCode, BigDecimal usdToKhrRate) {}
        try {
            ApiResponse<Quote> response = client.post().uri("/quote")
                    .body(new Request(businessId, customerId, points, currencyCode, usdToKhrRate))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            if (response == null || response.getData() == null || response.getData().discountAmount() == null) {
                throw new IllegalStateException("Customer service returned no loyalty quote");
            }
            return response.getData().discountAmount();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(exception.getStatusCode(), reason(exception));
            }
            throw exception;
        }
    }

    public Settlement settle(Long orderId, Long businessId, Long customerId, String orderNo, BigDecimal amount,
                             String currencyCode, BigDecimal usdToKhrRate, long redeemPoints) {
        record Request(Long businessId, Long customerId, String orderNo, BigDecimal amount, String currencyCode,
                       BigDecimal usdToKhrRate, long redeemPoints) {}
        ApiResponse<Settlement> response = client.post().uri("/orders/{orderId}/settle", orderId)
                .body(new Request(businessId, customerId, orderNo, amount, currencyCode, usdToKhrRate, redeemPoints))
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Customer service returned no loyalty settlement");
        }
        return response.getData();
    }

    private static String reason(RestClientResponseException exception) {
        try {
            ApiResponse<?> body = exception.getResponseBodyAs(ApiResponse.class);
            if (body != null && StringUtils.hasText(body.getMessage())) {
                return body.getMessage();
            }
        } catch (RuntimeException ignored) {
            // Fall back to a general reason when the customer service did not send the API envelope.
        }
        return "Loyalty points could not be redeemed";
    }

    public record Quote(long points, BigDecimal discountAmount) {
    }

    public record Settlement(long redeemedPoints, long earnedPoints, long balance) {
    }
}
