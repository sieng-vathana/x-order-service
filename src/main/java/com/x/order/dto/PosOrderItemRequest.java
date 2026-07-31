package com.x.order.dto;

import com.x.order.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PosOrderItemRequest(
        @NotNull @Positive Long variantId,
        @Positive int quantity,
        DiscountType discountType,
        @DecimalMin("0.00") BigDecimal discountValue,
        @Size(max = 500) String discountReason) {
}
