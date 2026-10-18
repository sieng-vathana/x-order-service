package com.x.order.messaging;

import com.x.order.event.OrderCancelledEvent;
import com.x.order.event.OrderCompletedEvent;
import com.x.order.event.OrderCreatedEvent;
import com.x.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class NotificationConsumer {

    private final TelegramOrderAlerts telegramOrderAlerts;

    @KafkaListener(topics = "x.order.events", groupId = "x-notification-group")
    public void onOrderEvent(OrderEvent event) {
        if (event instanceof OrderCreatedEvent created) {
            log.info("Notification: Order {} created at store {} (total: {} {})",
                    created.orderNo(), created.storeId(), created.grandTotal(), created.currencyCode());
            telegramOrderAlerts.onOrderCreated(created);
        } else if (event instanceof OrderCompletedEvent completed) {
            log.info("Notification: Order {} completed", completed.orderNo());
            telegramOrderAlerts.onOrderCompleted(completed);
        } else if (event instanceof OrderCancelledEvent cancelled) {
            log.info("Notification: Order {} cancelled", cancelled.orderNo());
        }
    }
}
