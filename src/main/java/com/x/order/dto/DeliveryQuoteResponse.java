package com.x.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryQuoteResponse(
        Long id, Long storeId, String providerType, BigDecimal distanceKm,
        BigDecimal quotedFee, String currencyCode, LocalDateTime expiresAt) {
}
