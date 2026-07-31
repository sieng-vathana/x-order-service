package com.x.order.event;

import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCancelledEvent(
        Long orderId,
        String orderNo,
        Long storeId,
        Long businessId,
        OrderChannel orderChannel,
        OrderStatus orderStatus,
        BigDecimal grandTotal,
        String currencyCode,
        LocalDateTime occurredAt) implements OrderEvent {

    public static OrderCancelledEvent from(com.x.order.entity.Order order) {
        return new OrderCancelledEvent(
                order.getId(),
                order.getOrderNo(),
                order.getStoreId(),
                order.getBusinessId(),
                order.getOrderChannel(),
                order.getOrderStatus(),
                order.getGrandTotal(),
                order.getCurrencyCode(),
                LocalDateTime.now());
    }
}
