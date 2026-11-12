package com.x.order.service;

import com.sharedlib.response.ApiResponse;
import com.x.order.dto.StockReservationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class InventoryClient {
    /** A till sale is paid within minutes. */
    static final Duration SALE_HOLD = Duration.ofMinutes(15);
    /** An online order waits for the store to prepare it and for the buyer to collect or receive it. */
    static final Duration ONLINE_ORDER_HOLD = Duration.ofDays(3);

    private final RestClient client;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${services.inventory.base-url:http://127.0.0.1:8085}") String baseUrl) {
        this.client = OrderServiceRestClient.create(builder, baseUrl, "/api/v1/inventory");
    }

    public StockReservationResponse reserve(Long storeId, Long variantId, Long orderId, long quantity) {
        return reserve(storeId, variantId, orderId, quantity, false);
    }

    public StockReservationResponse reserve(
            Long storeId, Long variantId, Long orderId, long quantity, boolean allowNegativeStock) {
        return reserve(storeId, variantId, orderId, quantity, allowNegativeStock, SALE_HOLD);
    }

    public StockReservationResponse reserveForOnlineOrder(Long storeId, Long variantId, Long orderId, long quantity) {
        return reserve(storeId, variantId, orderId, quantity, false, ONLINE_ORDER_HOLD);
    }

    private StockReservationResponse reserve(
            Long storeId, Long variantId, Long orderId, long quantity, boolean allowNegativeStock, Duration holdFor) {
        record Request(Long storeId, Long variantId, Long orderId, long quantity,
                       LocalDateTime expiresAt, boolean allowNegativeStock) {}
        try {
            ApiResponse<StockReservationResponse> response = client.post().uri("/reservations/with-details")
                    .body(new Request(storeId, variantId, orderId, quantity, LocalDateTime.now().plus(holdFor), allowNegativeStock))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("Inventory service returned no reservation");
            }
            return response.getData();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient available stock for store " + storeId + " and variant " + variantId);
            }
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(exception.getStatusCode(),
                        "Inventory rejected the stock reservation for variant " + variantId);
            }
            throw exception;
        }
    }

    /** Units that can still be sold: on hand minus reserved. */
    public long availableQuantity(Long storeId, Long variantId) {
        ApiResponse<StockLevel> response = client.get()
                .uri(uriBuilder -> uriBuilder.path("/balance")
                        .queryParam("storeId", storeId)
                        .queryParam("variantId", variantId)
                        .build())
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Inventory service returned no stock balance");
        }
        return response.getData().availableQuantity();
    }

    public record StockLevel(long availableQuantity) {
    }

    public void consume(Long reservationId) {
        client.post().uri("/reservations/{id}/consume", reservationId).retrieve().toBodilessEntity();
    }

    /** True when inventory refused to consume because the hold is no longer active (it expired or was released). */
    public static boolean holdNotActive(RuntimeException exception) {
        return exception instanceof RestClientResponseException response
                && response.getStatusCode().value() == HttpStatus.CONFLICT.value()
                && response.getResponseBodyAsString().contains("Only an active reservation");
    }

    public void release(Long reservationId) {
        client.delete().uri("/reservations/{id}", reservationId).retrieve().toBodilessEntity();
    }

    public void reopen(Long reservationId) {
        client.post().uri("/reservations/{id}/reopen", reservationId).retrieve().toBodilessEntity();
    }
}
