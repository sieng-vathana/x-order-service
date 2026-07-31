package com.x.order.dto;

import com.x.order.entity.*;

import java.math.BigDecimal;

public record AppliedDiscountResponse(
        Long id,
        Long discountId,
        String name,
        String code,
        DiscountMethod method,
        DiscountType type,
        DiscountScope scope,
        BigDecimal value,
        String currencyCode,
        BigDecimal amount,
        String reason,
        Long appliedBy) {
}
