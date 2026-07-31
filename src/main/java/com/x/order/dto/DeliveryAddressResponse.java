package com.x.order.dto;

import java.math.BigDecimal;

public record DeliveryAddressResponse(
        String receiverName, String phone, String provinceCity, String district, String commune,
        String village, String streetAddress, String landmark, String deliveryInstructions,
        BigDecimal latitude, BigDecimal longitude) {
}
