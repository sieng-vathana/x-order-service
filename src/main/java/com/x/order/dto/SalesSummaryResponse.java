package com.x.order.dto;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        long orderCount, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal grandTotal) {
}
