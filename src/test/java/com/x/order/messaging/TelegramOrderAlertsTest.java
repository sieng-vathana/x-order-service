package com.x.order.messaging;

import com.x.order.entity.DeliveryAddress;
import com.x.order.entity.Order;
import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderItem;
import com.x.order.entity.OrderStatus;
import com.x.order.event.OrderCompletedEvent;
import com.x.order.event.OrderCreatedEvent;
import com.x.order.repository.OrderRepository;
import com.x.order.service.InventoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramOrderAlertsTest {

    private OrderRepository orderRepository;
    private InventoryClient inventoryClient;
    private TelegramAlertClient telegramAlertClient;
    private TelegramOrderAlerts alerts;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        inventoryClient = mock(InventoryClient.class);
        telegramAlertClient = mock(TelegramAlertClient.class);
        alerts = new TelegramOrderAlerts(orderRepository, inventoryClient, telegramAlertClient);
        when(telegramAlertClient.settings(2L)).thenReturn(new TelegramAlertClient.AlertSettings(true, true, true, 5));
    }

    @Test
    void onlineOrderAlertListsItemsAndDelivery() {
        Order order = order(OrderChannel.ONLINE,
                item(11L, "Iced latte", "Large", 2),
                item(12L, "Croissant", "Croissant", 1));
        order.setDeliveryAddress(DeliveryAddress.builder()
                .receiverName("Dara").phone("012 345 678").provinceCity("Phnom Penh").build());
        when(orderRepository.findByIdWithItems(7L)).thenReturn(Optional.of(order));

        alerts.onOrderCreated(created(OrderChannel.ONLINE));

        verify(telegramAlertClient).send(2L, 3L, "ONLINE_ORDER", """
                🛒 New online order ORD-7
                $25.00 · 3 items
                • Iced latte (Large) x2
                • Croissant x1
                Deliver to: Dara · 012 345 678 · Phnom Penh""");
    }

    @Test
    void posOrdersAreNotAnnounced() {
        alerts.onOrderCreated(created(OrderChannel.POS));

        verify(telegramAlertClient, never()).settings(anyLong());
        verify(telegramAlertClient, never()).send(anyLong(), any(), anyString(), anyString());
    }

    @Test
    void lowStockIsReportedOnlyWhenThisSaleCrossesTheThreshold() {
        Order order = order(OrderChannel.POS,
                item(11L, "Iced latte", null, 2),   // 6 -> 4 available: crosses 5
                item(12L, "Croissant", null, 1),    // 4 -> 3 available: was already low
                item(13L, "Water", null, 1));       // 11 -> 10 available: still plenty
        when(orderRepository.findByIdWithItems(7L)).thenReturn(Optional.of(order));
        when(inventoryClient.availableQuantity(3L, 11L)).thenReturn(4L);
        when(inventoryClient.availableQuantity(3L, 12L)).thenReturn(3L);
        when(inventoryClient.availableQuantity(3L, 13L)).thenReturn(10L);

        alerts.onOrderCompleted(completed());

        verify(telegramAlertClient).send(2L, 3L, "LOW_STOCK", "⚠️ Low stock after order ORD-7\n• Iced latte: 4 left");
    }

    @Test
    void nothingIsCheckedWhenTelegramIsNotConnected() {
        when(telegramAlertClient.settings(2L)).thenReturn(new TelegramAlertClient.AlertSettings(false, true, true, 5));

        alerts.onOrderCompleted(completed());

        verify(orderRepository, never()).findByIdWithItems(any());
        verify(inventoryClient, never()).availableQuantity(any(), any());
    }

    @Test
    void textUsesRielWithoutDecimals() {
        Order order = order(OrderChannel.ONLINE, item(11L, "Iced latte", null, 1));
        order.setCurrencyCode("KHR");
        order.setGrandTotal(new BigDecimal("41000.00"));

        assertEquals("🛒 New online order ORD-7\n៛ 41,000 · 1 item\n• Iced latte x1", TelegramOrderAlerts.onlineOrderText(order));
    }

    private static Order order(OrderChannel channel, OrderItem... items) {
        Order order = Order.builder()
                .id(7L).orderNo("ORD-7").businessId(2L).storeId(3L).orderChannel(channel)
                .currencyCode("USD").grandTotal(new BigDecimal("25.00"))
                .items(List.of(items))
                .build();
        return order;
    }

    private static OrderItem item(Long variantId, String productName, String variantName, int qty) {
        return OrderItem.builder().variantId(variantId).productName(productName).variantName(variantName).qty(qty).build();
    }

    private static OrderCreatedEvent created(OrderChannel channel) {
        return new OrderCreatedEvent(7L, "ORD-7", 3L, 2L, 5L, null, channel, OrderStatus.DRAFT,
                new BigDecimal("25.00"), "USD", LocalDateTime.now());
    }

    private static OrderCompletedEvent completed() {
        return new OrderCompletedEvent(7L, "ORD-7", 3L, 2L, OrderChannel.POS, OrderStatus.COMPLETED,
                new BigDecimal("25.00"), "USD", LocalDateTime.now());
    }
}
