package com.x.order.service;

import com.x.order.dto.*;
import com.x.order.entity.*;
import com.x.order.repository.OrderRepository;
import com.x.order.event.OrderCancelledEvent;
import com.x.order.event.OrderCompletedEvent;
import com.x.order.event.OrderCreatedEvent;
import com.x.order.messaging.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final OrderRepository orderRepository;
    private final OrderPersistenceService persistenceService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final DiscountService discountService;
    private final DeliveryQuoteClient deliveryQuoteClient;

    @Autowired(required = false)
    private OrderEventProducer orderEventProducer;

    public OrderResponse createPos(CreatePosOrderRequest request) {
        String idempotencyKey = request.idempotencyKey().trim();
        Order existing = persistenceService.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) return toResponse(existing);

        validateDiscountRequest(request);
        String currency = normalizeCurrency(request.currencyCode());
        BigDecimal taxRate = money(request.taxRate() == null ? ZERO : request.taxRate());
        Order order = Order.builder()
                .businessId(request.businessId()).storeId(request.storeId()).customerId(request.customerId())
                .counterId(request.counterId()).cashierId(request.cashierId())
                .orderNo("POS-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT))
                .orderChannel(OrderChannel.POS).orderStatus(OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.UNPAID).currencyCode(currency)
                .idempotencyKey(idempotencyKey).discountType(request.discountType())
                .discountValue(money(request.discountValue() == null ? ZERO : request.discountValue()))
                .deliveryFee(ZERO).note(request.note()).items(new ArrayList<>()).coupons(new ArrayList<>()).build();

        BigDecimal subtotal = ZERO;
        BigDecimal lineDiscounts = ZERO;
        for (PosOrderItemRequest requestedItem : request.items()) {
            ProductVariantSaleResponse variant = productClient.getVariant(requestedItem.variantId());
            validateVariant(request, currency, variant);
            BigDecimal base = money(variant.posPrice().multiply(BigDecimal.valueOf(requestedItem.quantity())));
            BigDecimal discount = discount(
                    requestedItem.discountType(), itemDiscountValue(requestedItem), base);
            BigDecimal lineTax = money(base.subtract(discount).multiply(taxRate)
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            order.addItem(OrderItem.builder()
                    .productId(variant.productId()).variantId(variant.variantId())
                    .productName(variant.productName()).variantName(variant.variantName())
                    .sku(variant.sku()).barcode(variant.barcode()).qty(requestedItem.quantity())
                    .unitPrice(money(variant.posPrice())).discountType(requestedItem.discountType())
                    .discountValue(money(requestedItem.discountValue() == null ? ZERO : requestedItem.discountValue()))
                    .discountAmount(discount).orderDiscountAmount(ZERO).taxAmount(lineTax)
                    .total(money(base.subtract(discount).add(lineTax))).build());
            if (discount.signum() > 0) {
                OrderItem item = order.getItems().get(order.getItems().size() - 1);
                item.addDiscount(OrderItemDiscount.builder()
                        .name("Manual item discount")
                        .method(DiscountMethod.MANUAL)
                        .type(requestedItem.discountType())
                        .value(money(requestedItem.discountValue()))
                        .currencyCode(requestedItem.discountType() == DiscountType.FIXED ? currency : null)
                        .amount(discount)
                        .reason(requestedItem.discountReason().trim())
                        .appliedBy(request.cashierId())
                        .build());
            }
            subtotal = subtotal.add(base);
            lineDiscounts = lineDiscounts.add(discount);
        }
        BigDecimal afterLineDiscount = subtotal.subtract(lineDiscounts);
        BigDecimal orderDiscount = discount(request.discountType(), request.discountValue(), afterLineDiscount);
        BigDecimal totalDiscount = money(lineDiscounts.add(orderDiscount));
        allocateOrderDiscount(order, orderDiscount, taxRate);
        BigDecimal tax = money(order.getItems().stream()
                .map(OrderItem::getTaxAmount).reduce(ZERO, BigDecimal::add));
        order.setSubtotal(money(subtotal));
        order.setDiscount(totalDiscount);
        order.setTax(tax);
        order.setGrandTotal(money(subtotal.subtract(totalDiscount).add(tax)));
        if (orderDiscount.signum() > 0) {
            order.addDiscount(OrderDiscount.builder()
                    .name("Manual order discount")
                    .method(DiscountMethod.MANUAL)
                    .type(request.discountType())
                    .scope(DiscountScope.ORDER)
                    .value(money(request.discountValue()))
                    .currencyCode(request.discountType() == DiscountType.FIXED ? currency : null)
                    .amount(orderDiscount)
                    .reason(request.discountReason().trim())
                    .appliedBy(request.cashierId())
                    .build());
        }
        Order saved = hasText(request.discountCode())
                ? discountService.applyCodeAndSave(order, request.discountCode(), taxRate)
                : persistenceService.save(order);

        List<Long> reservations = new ArrayList<>();
        try {
            for (OrderItem item : saved.getItems()) {
                StockReservationResponse reservation = inventoryClient.reserve(
                        saved.getStoreId(), item.getVariantId(), saved.getId(), item.getQty());
                reservations.add(reservation.id());
                saved = persistenceService.attachReservation(saved.getId(), item.getId(), reservation.id());
            }
            publishOrderCreated(saved);
            return toResponse(saved);
        } catch (RuntimeException exception) {
            reservations.forEach(this::releaseQuietly);
            persistenceService.markCancelled(saved.getId());
            discountService.release(saved.getId());
            throw exception;
        }
    }

    public OrderResponse createOnline(CreateOnlineOrderRequest request) {
        String idempotencyKey = request.idempotencyKey().trim();
        Order existing = persistenceService.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) return toResponse(existing);

        String currency = normalizeCurrency(request.currencyCode());
        BigDecimal taxRate = money(request.taxRate() == null ? ZERO : request.taxRate());
        BigDecimal deliveryFee = resolveDeliveryFee(request, currency);
        Order order = Order.builder()
                .businessId(request.businessId()).storeId(request.storeId()).customerId(request.customerId())
                .orderNo("WEB-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT))
                .orderChannel(OrderChannel.ONLINE).orderStatus(OrderStatus.PENDING_CONFIRMATION)
                .paymentStatus(request.paymentType() == OnlinePaymentType.COD
                        ? PaymentStatus.COD_PENDING : PaymentStatus.UNPAID)
                .currencyCode(currency).idempotencyKey(idempotencyKey)
                .deliveryFee(deliveryFee).fulfillmentType(request.fulfillmentType())
                .note(request.note()).items(new ArrayList<>()).coupons(new ArrayList<>()).discounts(new ArrayList<>())
                .build();
        if (request.deliveryAddress() != null) attachDeliveryAddress(order, request.deliveryAddress());

        BigDecimal subtotal = ZERO;
        for (OnlineOrderItemRequest requestedItem : request.items()) {
            ProductVariantSaleResponse variant = productClient.getVariant(requestedItem.variantId());
            validateOnlineVariant(request, currency, variant);
            BigDecimal base = money(variant.onlinePrice().multiply(BigDecimal.valueOf(requestedItem.quantity())));
            BigDecimal tax = money(base.multiply(taxRate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            order.addItem(OrderItem.builder()
                    .productId(variant.productId()).variantId(variant.variantId())
                    .productName(variant.productName()).variantName(variant.variantName())
                    .sku(variant.sku()).barcode(variant.barcode()).qty(requestedItem.quantity())
                    .unitPrice(money(variant.onlinePrice())).discountAmount(ZERO).orderDiscountAmount(ZERO)
                    .taxAmount(tax).total(money(base.add(tax))).build());
            subtotal = subtotal.add(base);
        }
        order.setSubtotal(money(subtotal));
        order.setDiscount(ZERO);
        order.setTax(money(order.getItems().stream().map(OrderItem::getTaxAmount).reduce(ZERO, BigDecimal::add)));
        order.setGrandTotal(money(order.getSubtotal().add(order.getTax()).add(deliveryFee)));
        Order saved = hasText(request.discountCode())
                ? discountService.applyCodeAndSave(order, request.discountCode(), taxRate)
                : persistenceService.save(order);

        List<Long> reservations = new ArrayList<>();
        try {
            for (OrderItem item : saved.getItems()) {
                StockReservationResponse reservation = inventoryClient.reserve(
                        saved.getStoreId(), item.getVariantId(), saved.getId(), item.getQty());
                reservations.add(reservation.id());
                saved = persistenceService.attachReservation(saved.getId(), item.getId(), reservation.id());
            }
            publishOrderCreated(saved);
            return toResponse(saved);
        } catch (RuntimeException exception) {
            reservations.forEach(this::releaseQuietly);
            persistenceService.markCancelled(saved.getId());
            discountService.release(saved.getId());
            throw exception;
        }
    }

    public OrderResponse confirmOnline(Long id) {
        return toResponse(persistenceService.confirmOnline(id));
    }

    public OrderResponse complete(Long id) {
        Order order = persistenceService.require(id);
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            discountService.redeem(id);
            return toResponse(order);
        }
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is not awaiting payment");
        }
        BigDecimal paid = paymentClient.listForOrder(id).stream()
                .filter(payment -> "PAID".equals(payment.status()) || "PARTIALLY_REFUNDED".equals(payment.status()))
                .map(payment -> payment.amount().subtract(
                        payment.refundedAmount() == null ? ZERO : payment.refundedAmount()))
                .reduce(ZERO, BigDecimal::add);
        if (paid.compareTo(order.getGrandTotal()) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order payment is incomplete");
        }
        List<Long> consumedReservations = new ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                if (item.getReservationId() == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Order item has no stock reservation");
                }
                inventoryClient.consume(item.getReservationId());
                consumedReservations.add(item.getReservationId());
            }
        } catch (RuntimeException exception) {
            consumedReservations.forEach(this::reopenQuietly);
            throw exception;
        }
        Order completed;
        try {
            completed = persistenceService.markCompleted(id);
        } catch (RuntimeException exception) {
            consumedReservations.forEach(this::reopenQuietly);
            throw exception;
        }
        discountService.redeem(id);
        publishOrderCompleted(completed);
        return toResponse(completed);
    }

    public OrderResponse cancel(Long id) {
        Order order = persistenceService.require(id);
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            discountService.release(id);
            return toResponse(order);
        }
        order.getItems().stream().map(OrderItem::getReservationId).filter(Objects::nonNull)
                .forEach(this::releaseQuietly);
        Order cancelled = persistenceService.markCancelled(id);
        discountService.release(id);
        publishOrderCancelled(cancelled);
        return toResponse(cancelled);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return toResponse(persistenceService.require(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(Long storeId, int page, int size) {
        return orderRepository.findByStoreId(
                storeId, PageRequest.of(page, size, Sort.by("createdAt").descending())).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(Long storeId, LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        var row = orderRepository.salesSummary(storeId, OrderStatus.COMPLETED, from, to);
        return new SalesSummaryResponse(row.getOrderCount(), row.getSubtotal(), row.getDiscount(),
                row.getTax(), row.getGrandTotal());
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(Long storeId, LocalDateTime from, LocalDateTime to, int limit) {
        validateRange(from, to);
        return orderRepository.topProducts(storeId, OrderStatus.COMPLETED, from, to, PageRequest.of(0, limit))
                .stream().map(row -> new TopProductResponse(
                        row.getVariantId(), row.getProductName(), row.getSku(),
                        row.getQuantity(), row.getSalesAmount())).toList();
    }

    private void validateVariant(CreatePosOrderRequest request, String currency, ProductVariantSaleResponse variant) {
        if (!request.storeId().equals(variant.storeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variant belongs to another store");
        }
        if (!currency.equalsIgnoreCase(variant.currencyCode()) || variant.posPrice() == null
                || Integer.valueOf(2).equals(variant.salesChannel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variant is unavailable for this POS order");
        }
    }

    private void validateOnlineVariant(CreateOnlineOrderRequest request, String currency,
                                       ProductVariantSaleResponse variant) {
        if (!request.storeId().equals(variant.storeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variant belongs to another store");
        }
        if (!currency.equalsIgnoreCase(variant.currencyCode()) || variant.onlinePrice() == null
                || Integer.valueOf(1).equals(variant.salesChannel())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variant is unavailable for this online order");
        }
    }

    private BigDecimal resolveDeliveryFee(CreateOnlineOrderRequest request, String currency) {
        if (request.fulfillmentType() == FulfillmentType.CUSTOMER_PICKUP) return ZERO;
        DeliveryQuoteResponse quote = deliveryQuoteClient.getQuote(request.deliveryQuoteId());
        if (!request.storeId().equals(quote.storeId()) || !currency.equalsIgnoreCase(quote.currencyCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery quote does not match this store or currency");
        }
        return money(quote.quotedFee());
    }

    private void attachDeliveryAddress(Order order, DeliveryAddressRequest request) {
        DeliveryAddress address = DeliveryAddress.builder()
                .order(order).receiverName(request.receiverName()).phone(request.phone())
                .provinceCity(request.provinceCity()).district(request.district()).commune(request.commune())
                .village(request.village()).streetAddress(request.streetAddress()).landmark(request.landmark())
                .deliveryInstructions(request.deliveryInstructions()).latitude(request.latitude())
                .longitude(request.longitude()).build();
        order.setDeliveryAddress(address);
    }

    private BigDecimal discount(DiscountType type, BigDecimal rawValue, BigDecimal base) {
        BigDecimal value = money(rawValue == null ? ZERO : rawValue);
        if (type == null) {
            if (value.signum() != 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount type is required");
            return ZERO;
        }
        BigDecimal amount;
        if (type == DiscountType.PERCENTAGE) {
            if (value.compareTo(new BigDecimal("100.00")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100");
            }
            amount = base.multiply(value).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        } else {
            amount = value;
        }
        if (amount.compareTo(base) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount cannot exceed item/order amount");
        }
        return money(amount);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> new OrderItemResponse(
                item.getId(), item.getProductId(), item.getVariantId(), item.getReservationId(), item.getProductName(),
                item.getVariantName(), item.getSku(), item.getBarcode(), item.getQty(), item.getUnitPrice(),
                item.getDiscountType(), item.getDiscountValue(), item.getDiscountAmount(),
                item.getOrderDiscountAmount(), item.getTaxAmount(), item.getTotal(), item.getDiscounts().stream()
                .map(discount -> toResponse(discount, DiscountScope.ITEM)).toList())).toList();
        return new OrderResponse(
                order.getId(), order.getOrderNo(), order.getBusinessId(), order.getStoreId(), order.getCustomerId(),
                order.getCounterId(), order.getCashierId(), order.getOrderChannel(), order.getOrderStatus(),
                order.getPaymentStatus(), order.getCurrencyCode(), order.getSubtotal(), order.getDiscountType(),
                order.getDiscountValue(), order.getDiscount(), order.getTax(), order.getDeliveryFee(), order.getGrandTotal(),
                order.getFulfillmentType(), toResponse(order.getDeliveryAddress()), order.getNote(),
                order.getDiscounts().stream().map(this::toResponse).toList(),
                items, order.getCreatedAt(), order.getCompletedAt());
    }

    private DeliveryAddressResponse toResponse(DeliveryAddress address) {
        if (address == null) return null;
        return new DeliveryAddressResponse(address.getReceiverName(), address.getPhone(), address.getProvinceCity(),
                address.getDistrict(), address.getCommune(), address.getVillage(), address.getStreetAddress(),
                address.getLandmark(), address.getDeliveryInstructions(), address.getLatitude(), address.getLongitude());
    }

    private AppliedDiscountResponse toResponse(OrderDiscount discount) {
        return new AppliedDiscountResponse(
                discount.getId(), discount.getDiscountId(), discount.getName(), discount.getCode(),
                discount.getMethod(), discount.getType(), discount.getScope(), discount.getValue(),
                discount.getCurrencyCode(), discount.getAmount(), discount.getReason(), discount.getAppliedBy());
    }

    private AppliedDiscountResponse toResponse(
            OrderItemDiscount discount, DiscountScope scope) {
        return new AppliedDiscountResponse(
                discount.getId(), discount.getDiscountId(), discount.getName(), discount.getCode(),
                discount.getMethod(), discount.getType(), scope, discount.getValue(),
                discount.getCurrencyCode(), discount.getAmount(), discount.getReason(), discount.getAppliedBy());
    }

    private void validateDiscountRequest(CreatePosOrderRequest request) {
        validateManualDiscount(
                request.discountType(), request.discountValue(), request.discountReason(), "Order");
        request.items().forEach(item -> validateManualDiscount(
                item.discountType(), item.discountValue(), item.discountReason(), "Item"));
    }

    private void validateManualDiscount(
            DiscountType type, BigDecimal rawValue, String reason, String target) {
        BigDecimal value = money(rawValue == null ? ZERO : rawValue);
        if (type == null && value.signum() == 0) {
            if (hasText(reason)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, target + " discount reason has no discount");
            }
            return;
        }
        if (type == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, target + " discount type is required");
        }
        if (value.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, target + " discount value must be greater than zero");
        }
        if (!hasText(reason)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, target + " discount reason is required");
        }
    }

    private boolean hasManualDiscount(DiscountType type, BigDecimal rawValue) {
        return type != null || (rawValue != null && rawValue.signum() != 0);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal itemDiscountValue(PosOrderItemRequest item) {
        if (item.discountType() == DiscountType.FIXED && item.discountValue() != null) {
            return item.discountValue().multiply(BigDecimal.valueOf(item.quantity()));
        }
        return item.discountValue();
    }

    private void allocateOrderDiscount(
            Order order, BigDecimal orderDiscount, BigDecimal taxRate) {
        List<OrderItem> eligibleItems = order.getItems().stream()
                .filter(item -> itemBaseAfterLineDiscount(item).signum() > 0)
                .toList();
        BigDecimal eligibleBase = eligibleItems.stream()
                .map(this::itemBaseAfterLineDiscount)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal remaining = orderDiscount;

        for (OrderItem item : order.getItems()) {
            BigDecimal lineBase = itemBaseAfterLineDiscount(item);
            BigDecimal allocation = ZERO;
            int eligibleIndex = eligibleItems.indexOf(item);
            if (eligibleIndex >= 0 && orderDiscount.signum() > 0) {
                allocation = eligibleIndex == eligibleItems.size() - 1
                        ? remaining
                        : money(orderDiscount.multiply(lineBase)
                        .divide(eligibleBase, 4, RoundingMode.HALF_UP));
                allocation = allocation.min(lineBase).min(remaining);
                remaining = remaining.subtract(allocation);
            }
            item.setOrderDiscountAmount(money(allocation));
            item.setTaxAmount(money(lineBase.subtract(allocation).multiply(taxRate)
                    .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)));
            item.setTotal(money(lineBase.subtract(allocation).add(item.getTaxAmount())));
        }
    }

    private BigDecimal itemBaseAfterLineDiscount(OrderItem item) {
        BigDecimal base = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty()));
        BigDecimal lineDiscount = item.getDiscountAmount() == null ? ZERO : item.getDiscountAmount();
        return money(base.subtract(lineDiscount));
    }

    private String normalizeCurrency(String code) {
        String value = code.toUpperCase(Locale.ROOT);
        try { Currency.getInstance(value); return value; }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ISO 4217 currency code");
        }
    }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private void releaseQuietly(Long id) { try { inventoryClient.release(id); } catch (RuntimeException ignored) { } }
    private void reopenQuietly(Long id) { try { inventoryClient.reopen(id); } catch (RuntimeException ignored) { } }
    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
    }

    private void publishOrderCreated(Order order) {
        if (orderEventProducer != null) {
            try {
                orderEventProducer.publish(OrderCreatedEvent.from(order));
            } catch (RuntimeException ignored) { }
        }
    }

    private void publishOrderCompleted(Order order) {
        if (orderEventProducer != null) {
            try {
                orderEventProducer.publish(OrderCompletedEvent.from(order));
            } catch (RuntimeException ignored) { }
        }
    }

    private void publishOrderCancelled(Order order) {
        if (orderEventProducer != null) {
            try {
                orderEventProducer.publish(OrderCancelledEvent.from(order));
            } catch (RuntimeException ignored) { }
        }
    }
}
