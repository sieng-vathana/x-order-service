package com.x.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OnlineOrderItemRequest(
        @NotNull @Positive Long variantId,
        @Positive int quantity) {
}
