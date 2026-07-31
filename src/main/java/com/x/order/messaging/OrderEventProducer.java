package com.x.order.messaging;

import com.x.order.event.OrderCancelledEvent;
import com.x.order.event.OrderCompletedEvent;
import com.x.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class OrderEventProducer {

    private static final String TOPIC = "x.order.events";

    private final KafkaTemplate<String, Object> orderEventKafkaTemplate;

    public void publish(OrderCreatedEvent event) {
        send("OrderCreated", event.orderId().toString(), event);
    }

    public void publish(OrderCompletedEvent event) {
        send("OrderCompleted", event.orderId().toString(), event);
    }

    public void publish(OrderCancelledEvent event) {
        send("OrderCancelled", event.orderId().toString(), event);
    }

    private void send(String eventType, String key, Object payload) {
        orderEventKafkaTemplate.send(TOPIC, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish {} event for key={}", eventType, key, ex);
                    } else {
                        log.debug("Published {} event for key={} to {}", eventType, key,
                                result.getRecordMetadata().topic());
                    }
                });
    }
}
