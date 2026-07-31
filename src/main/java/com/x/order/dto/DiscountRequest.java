package com.x.order.dto;

import com.x.order.entity.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record DiscountRequest(
        @NotNull @Positive Long businessId,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,64}") String code,
        @NotNull DiscountType type,
        @NotNull DiscountScope scope,
        @NotNull DiscountChannel channel,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal value,
        @Pattern(regexp = "[A-Za-z]{3}") String currencyCode,
        @DecimalMin("0.00") BigDecimal minimumOrderAmount,
        @DecimalMin("0.00") BigDecimal maximumDiscountAmount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        @Positive Integer usageLimit,
        @Positive Integer usageLimitPerCustomer,
        boolean combinable,
        DiscountStatus status,
        @Size(max = 500) Set<@Positive Long> storeIds,
        @Size(max = 2000) Set<@Positive Long> productIds,
        @Size(max = 2000) Set<@Positive Long> variantIds) {
}
