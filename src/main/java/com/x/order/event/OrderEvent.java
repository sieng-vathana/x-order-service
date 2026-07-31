package com.x.order.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "OrderCreated"),
        @JsonSubTypes.Type(value = OrderCompletedEvent.class, name = "OrderCompleted"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "OrderCancelled")
})
public sealed interface OrderEvent permits OrderCreatedEvent, OrderCompletedEvent, OrderCancelledEvent {

    Long orderId();

    String orderNo();

    Long storeId();

    Long businessId();

    OrderChannel orderChannel();

    OrderStatus orderStatus();

    BigDecimal grandTotal();

    String currencyCode();

    LocalDateTime occurredAt();
}
