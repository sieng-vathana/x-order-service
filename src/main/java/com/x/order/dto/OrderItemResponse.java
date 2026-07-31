package com.x.order.dto;

import com.x.order.entity.DiscountType;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponse(
        Long id, Long productId, Long variantId, Long reservationId, String productName, String variantName,
        String sku, String barcode, int quantity, BigDecimal unitPrice,
        DiscountType discountType, BigDecimal discountValue, BigDecimal discountAmount,
        BigDecimal orderDiscountAmount, BigDecimal taxAmount, BigDecimal total,
        List<AppliedDiscountResponse> discounts) {
}
