package com.x.order.dto;

import com.x.order.entity.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreatePosOrderRequest(
        @NotNull @Positive Long businessId,
        @NotNull @Positive Long storeId,
        @Positive Long customerId,
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
        @NotEmpty List<@Valid PosOrderItemRequest> items) {
}
