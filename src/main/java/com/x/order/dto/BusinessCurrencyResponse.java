package com.x.order.dto;

import java.math.BigDecimal;

/** Minimal business settings needed by the order service for POS currency conversion. */
public record BusinessCurrencyResponse(
        Long id,
        String defaultCurrencyCode,
        BigDecimal usdToKhrExchangeRate) {
}
