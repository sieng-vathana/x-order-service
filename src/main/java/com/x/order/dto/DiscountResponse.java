package com.x.order.dto;

import com.x.order.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record DiscountResponse(
        Long id,
        Long businessId,
        String name,
        String code,
        DiscountMethod method,
        DiscountType type,
        DiscountScope scope,
        DiscountChannel channel,
        BigDecimal value,
        String currencyCode,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer usageLimit,
        Integer usageLimitPerCustomer,
        long usedCount,
        boolean combinable,
        DiscountStatus status,
        Set<Long> storeIds,
        Set<Long> productIds,
        Set<Long> variantIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
