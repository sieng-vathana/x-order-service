package com.x.order.dto;

import java.math.BigDecimal;

/** Minimal business settings needed by the order service for POS currency conversion. */
public record BusinessCurrencyResponse(
        Long id,
        String defaultCurrencyCode,
        BigDecimal usdToKhrExchangeRate,
        Integer status,
        Boolean suspended,
        String suspensionReason) {

    public BusinessCurrencyResponse(Long id, String defaultCurrencyCode, BigDecimal usdToKhrExchangeRate) {
        this(id, defaultCurrencyCode, usdToKhrExchangeRate, 1, false, null);
    }

    public BusinessCurrencyResponse(Long id, String defaultCurrencyCode, BigDecimal usdToKhrExchangeRate, Integer status) {
        this(id, defaultCurrencyCode, usdToKhrExchangeRate, status, false, null);
    }
}
