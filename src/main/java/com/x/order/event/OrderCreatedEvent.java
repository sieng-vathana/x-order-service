package com.x.order.event;

import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        String orderNo,
        Long storeId,
        Long businessId,
        Long customerId,
        Long cashierId,
        OrderChannel orderChannel,
        OrderStatus orderStatus,
        BigDecimal grandTotal,
        String currencyCode,
        LocalDateTime occurredAt) implements OrderEvent {

    public static OrderCreatedEvent from(com.x.order.entity.Order order) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getOrderNo(),
                order.getStoreId(),
                order.getBusinessId(),
                order.getCustomerId(),
                order.getCashierId(),
                order.getOrderChannel(),
                order.getOrderStatus(),
                order.getGrandTotal(),
                order.getCurrencyCode(),
                LocalDateTime.now());
    }
}
