package com.x.order.dto;

import com.x.order.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id, String orderNo, Long businessId, Long storeId, Long customerId, Long counterId,
        Long cashierId, OrderChannel orderChannel, OrderStatus orderStatus, PaymentStatus paymentStatus,
        String currencyCode, BigDecimal subtotal, DiscountType discountType, BigDecimal discountValue,
        BigDecimal discount, BigDecimal tax, BigDecimal deliveryFee, BigDecimal grandTotal,
        FulfillmentType fulfillmentType, DeliveryAddressResponse deliveryAddress, String note,
        List<AppliedDiscountResponse> discounts, List<OrderItemResponse> items,
        LocalDateTime createdAt, LocalDateTime completedAt, Long loyaltyPointsRedeemed, Long loyaltyPointsEarned) {
}
