package com.x.order.repository;

import java.math.BigDecimal;

public interface TopProductProjection {
    Long getVariantId();
    String getProductName();
    String getSku();
    Long getQuantity();
    BigDecimal getSalesAmount();
}
