package com.x.order.repository;

import java.math.BigDecimal;

public interface SalesSummaryProjection {
    Long getOrderCount();
    BigDecimal getSubtotal();
    BigDecimal getDiscount();
    BigDecimal getTax();
    BigDecimal getGrandTotal();
}
