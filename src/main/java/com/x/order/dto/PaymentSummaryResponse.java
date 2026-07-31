package com.x.order.dto;

import java.math.BigDecimal;

public record PaymentSummaryResponse(
        Long id, Long orderId, BigDecimal amount, BigDecimal refundedAmount, String status) {
}
