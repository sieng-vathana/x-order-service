package com.x.order.dto;

import java.math.BigDecimal;

/** Completed sales in one hour, day or month, in the viewer's time zone (period is "2026-10-20 14:00", "2026-10-20" or "2026-10"). */
public record SalesBreakdownResponse(
        String period, long orderCount, BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal grandTotal) {
}
