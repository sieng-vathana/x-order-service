package com.x.order.dto;

import java.math.BigDecimal;

public record ProductVariantSaleResponse(
        Long productId, Long variantId, Long storeId, String productName, String variantName,
        String sku, String barcode, String currencyCode, Integer salesChannel,
        BigDecimal costPrice, BigDecimal posPrice, BigDecimal onlinePrice, Integer status) {
}
