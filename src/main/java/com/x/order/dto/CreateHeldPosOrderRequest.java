package com.x.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;
import java.util.List;

public record CreateHeldPosOrderRequest(
        @NotNull @Positive Long businessId,
        @NotNull @Positive Long storeId,
        @PositiveOrZero Long customerId,
        @Positive Long counterId,
        @NotNull @Positive Long cashierId,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currencyCode,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
        @Size(max = 500) String note,
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotEmpty List<@Valid PosOrderItemRequest> items,
        @Min(0) @Max(100) Integer roundingIncrement,
        Boolean allowNegativeStock) {

    public CreateHeldPosOrderRequest(
            Long businessId, Long storeId, Long customerId, Long counterId, Long cashierId,
            String currencyCode, BigDecimal taxRate, String note, String idempotencyKey,
            List<PosOrderItemRequest> items) {
        this(businessId, storeId, customerId, counterId, cashierId, currencyCode, taxRate, note,
                idempotencyKey, items, null, false);
    }
}
