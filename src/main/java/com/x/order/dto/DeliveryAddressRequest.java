package com.x.order.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DeliveryAddressRequest(
        @NotBlank @Size(max = 160) String receiverName,
        @NotBlank @Size(max = 32) String phone,
        @NotBlank @Size(max = 100) String provinceCity,
        @Size(max = 100) String district,
        @Size(max = 100) String commune,
        @Size(max = 100) String village,
        @NotBlank @Size(max = 255) String streetAddress,
        @Size(max = 255) String landmark,
        @Size(max = 500) String deliveryInstructions,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
}
