package com.x.order.dto;

import java.math.BigDecimal;

public record TopProductResponse(
        Long variantId, String productName, String sku, long quantity, BigDecimal salesAmount) {
}
