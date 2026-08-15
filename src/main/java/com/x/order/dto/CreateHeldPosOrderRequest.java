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
        @NotEmpty List<@Valid PosOrderItemRequest> items) {
}
