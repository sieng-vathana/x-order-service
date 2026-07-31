package com.x.order.service;

import com.sharedlib.response.ApiResponse;
import com.x.order.dto.StockReservationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Component
public class InventoryClient {
    private final RestClient client;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${services.inventory.base-url:http://127.0.0.1:8085}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl + "/api/v1/inventory").build();
    }

    public StockReservationResponse reserve(Long storeId, Long variantId, Long orderId, long quantity) {
        record Request(Long storeId, Long variantId, Long orderId, long quantity, LocalDateTime expiresAt) {}
        ApiResponse<StockReservationResponse> response = client.post().uri("/reservations/with-details")
                .body(new Request(storeId, variantId, orderId, quantity, LocalDateTime.now().plusMinutes(15)))
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Inventory service returned no reservation");
        }
        return response.getData();
    }

    public void consume(Long reservationId) {
        client.post().uri("/reservations/{id}/consume", reservationId).retrieve().toBodilessEntity();
    }

    public void release(Long reservationId) {
        client.delete().uri("/reservations/{id}", reservationId).retrieve().toBodilessEntity();
    }

    public void reopen(Long reservationId) {
        client.post().uri("/reservations/{id}/reopen", reservationId).retrieve().toBodilessEntity();
    }
}
