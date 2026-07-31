package com.x.order.dto;

import java.time.LocalDateTime;

public record StockReservationResponse(
        Long id, Long storeId, Long variantId, Long orderId, long quantity,
        String status, LocalDateTime expiresAt, Object balance) {
}
