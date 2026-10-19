package com.x.order.repository;

import java.math.BigDecimal;

public interface SalesBreakdownProjection {
    String getPeriod();
    Long getOrderCount();
    BigDecimal getSubtotal();
    BigDecimal getDiscount();
    BigDecimal getTax();
    BigDecimal getGrandTotal();
}
