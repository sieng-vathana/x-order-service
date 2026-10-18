package com.x.order.dto;

import com.x.order.entity.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreatePosOrderRequest(
        @NotNull @Positive Long businessId,
        @NotNull @Positive Long storeId,
        @PositiveOrZero Long customerId,
        @Positive Long counterId,
        @NotNull @Positive Long cashierId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currencyCode,
        DiscountType discountType,
        @DecimalMin("0.00") BigDecimal discountValue,
        @Size(max = 500) String discountReason,
        @Pattern(regexp = "[A-Za-z0-9_-]{2,64}") String discountCode,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
        @Size(max = 500) String note,
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotEmpty List<@Valid PosOrderItemRequest> items,
        @Min(0) @Max(100) Integer roundingIncrement,
        Boolean allowNegativeStock,
        /** Points the selected customer spends as an order discount; checked with the customer service. */
        @PositiveOrZero Long loyaltyPointsToRedeem) {

    public CreatePosOrderRequest(
            Long businessId, Long storeId, Long customerId, Long counterId, Long cashierId,
            String currencyCode, DiscountType discountType, BigDecimal discountValue, String discountReason,
            String discountCode, BigDecimal taxRate, String note, String idempotencyKey,
            List<PosOrderItemRequest> items) {
        this(businessId, storeId, customerId, counterId, cashierId, currencyCode, discountType, discountValue,
                discountReason, discountCode, taxRate, note, idempotencyKey, items, null, false, null);
    }
}
