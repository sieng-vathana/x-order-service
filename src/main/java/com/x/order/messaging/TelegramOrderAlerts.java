package com.x.order.messaging;

import com.x.order.entity.DeliveryAddress;
import com.x.order.entity.Order;
import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderItem;
import com.x.order.event.OrderCompletedEvent;
import com.x.order.event.OrderCreatedEvent;
import com.x.order.repository.OrderRepository;
import com.x.order.service.InventoryClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** Sends new online orders, and products a sale left low in stock, to the merchant's Telegram chat. */
@Component
@RequiredArgsConstructor
public class TelegramOrderAlerts {

    private static final Logger log = LoggerFactory.getLogger(TelegramOrderAlerts.class);
    private static final int MAX_ITEM_LINES = 10;

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final TelegramAlertClient telegramAlertClient;

    @Transactional(readOnly = true)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event.orderChannel() == OrderChannel.POS) {
            return;
        }
        try {
            TelegramAlertClient.AlertSettings settings = telegramAlertClient.settings(event.businessId());
            if (!settings.connected() || !settings.orderAlerts()) {
                return;
            }
            orderRepository.findByIdWithItems(event.orderId())
                    .ifPresent(order -> telegramAlertClient.send(order.getBusinessId(), order.getStoreId(), "ONLINE_ORDER", onlineOrderText(order)));
        } catch (RuntimeException exception) {
            log.warn("Online order alert for {} failed: {}", event.orderNo(), exception.getMessage());
        }
    }

    /** A product is reported once: when this sale takes its available stock to the threshold or below. */
    @Transactional(readOnly = true)
    public void onOrderCompleted(OrderCompletedEvent event) {
        try {
            TelegramAlertClient.AlertSettings settings = telegramAlertClient.settings(event.businessId());
            if (!settings.connected() || !settings.lowStockAlerts()) {
                return;
            }
            Order order = orderRepository.findByIdWithItems(event.orderId()).orElse(null);
            if (order == null) {
                return;
            }
            Map<Long, Integer> soldByVariant = new LinkedHashMap<>();
            Map<Long, String> nameByVariant = new LinkedHashMap<>();
            for (OrderItem item : order.getItems()) {
                if (item.getVariantId() == null) {
                    continue;
                }
                soldByVariant.merge(item.getVariantId(), item.getQty() == null ? 0 : item.getQty(), Integer::sum);
                nameByVariant.putIfAbsent(item.getVariantId(), itemName(item));
            }
            List<String> lowLines = new ArrayList<>();
            int threshold = settings.lowStockThreshold();
            soldByVariant.forEach((variantId, sold) -> {
                long available = inventoryClient.availableQuantity(order.getStoreId(), variantId);
                if (available <= threshold && available + sold > threshold) {
                    lowLines.add("• " + nameByVariant.get(variantId) + ": " + available + " left");
                }
            });
            if (!lowLines.isEmpty()) {
                telegramAlertClient.send(order.getBusinessId(), order.getStoreId(), "LOW_STOCK",
                        "⚠️ Low stock after order " + order.getOrderNo() + "\n" + String.join("\n", lowLines));
            }
        } catch (RuntimeException exception) {
            log.warn("Low stock alert for {} failed: {}", event.orderNo(), exception.getMessage());
        }
    }

    static String onlineOrderText(Order order) {
        List<OrderItem> items = order.getItems();
        int units = items.stream().mapToInt(item -> item.getQty() == null ? 0 : item.getQty()).sum();
        StringBuilder text = new StringBuilder("🛒 New online order ").append(order.getOrderNo()).append('\n')
                .append(money(order.getGrandTotal(), order.getCurrencyCode()))
                .append(" · ").append(units).append(units == 1 ? " item" : " items");
        items.stream().limit(MAX_ITEM_LINES)
                .forEach(item -> text.append("\n• ").append(itemName(item)).append(" x").append(item.getQty()));
        if (items.size() > MAX_ITEM_LINES) {
            text.append("\n…and ").append(items.size() - MAX_ITEM_LINES).append(" more");
        }
        DeliveryAddress address = order.getDeliveryAddress();
        if (address != null) {
            String deliverTo = String.join(" · ", Stream.of(address.getReceiverName(), address.getPhone(), address.getProvinceCity())
                    .filter(StringUtils::hasText)
                    .toList());
            if (!deliverTo.isEmpty()) {
                text.append("\nDeliver to: ").append(deliverTo);
            }
        }
        return text.toString();
    }

    private static String itemName(OrderItem item) {
        String product = StringUtils.hasText(item.getProductName()) ? item.getProductName() : "Item";
        return StringUtils.hasText(item.getVariantName()) && !item.getVariantName().equals(item.getProductName())
                ? product + " (" + item.getVariantName() + ")"
                : product;
    }

    private static String money(BigDecimal amount, String currency) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        if ("KHR".equals(currency)) {
            return "៛ " + new DecimalFormat("#,##0", symbols).format(value.setScale(0, RoundingMode.HALF_UP));
        }
        String formatted = new DecimalFormat("#,##0.00", symbols).format(value.setScale(2, RoundingMode.HALF_UP));
        return "USD".equals(currency) ? "$" + formatted : formatted + " " + currency;
    }
}
