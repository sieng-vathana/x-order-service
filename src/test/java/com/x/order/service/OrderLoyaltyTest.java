package com.x.order.service;

import com.x.order.dto.CreatePosOrderRequest;
import com.x.order.dto.PaymentSummaryResponse;
import com.x.order.dto.PosOrderItemRequest;
import com.x.order.dto.ProductVariantSaleResponse;
import com.x.order.dto.StockReservationResponse;
import com.x.order.entity.DiscountMethod;
import com.x.order.entity.Order;
import com.x.order.entity.OrderChannel;
import com.x.order.entity.OrderItem;
import com.x.order.entity.OrderStatus;
import com.x.order.entity.PaymentStatus;
import com.x.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderLoyaltyTest {

    private OrderPersistenceService persistenceService;
    private ProductClient productClient;
    private InventoryClient inventoryClient;
    private PaymentClient paymentClient;
    private LoyaltyClient loyaltyClient;
    private OrderService service;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        persistenceService = mock(OrderPersistenceService.class);
        productClient = mock(ProductClient.class);
        inventoryClient = mock(InventoryClient.class);
        paymentClient = mock(PaymentClient.class);
        loyaltyClient = mock(LoyaltyClient.class);
        service = new OrderService(mock(OrderRepository.class), persistenceService, productClient, mock(BusinessClient.class),
                inventoryClient, paymentClient, mock(DiscountService.class), mock(DeliveryQuoteClient.class), loyaltyClient);
        when(productClient.getVariant(11L, 3L)).thenReturn(new ProductVariantSaleResponse(
                1L, 11L, 3L, "Iced latte", "Large", "SKU-1", null, "USD", 0,
                new BigDecimal("1.00"), new BigDecimal("2.50"), new BigDecimal("2.50"), 1));
        when(persistenceService.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(persistenceService.save(any(Order.class))).thenAnswer(invocation -> {
            savedOrder = invocation.getArgument(0);
            savedOrder.setId(50L);
            long itemId = 500;
            for (OrderItem item : savedOrder.getItems()) item.setId(itemId++);
            return savedOrder;
        });
        when(inventoryClient.reserve(anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean()))
                .thenReturn(new StockReservationResponse(70L, 3L, 11L, 50L, 4, "ACTIVE", null, null));
        when(persistenceService.attachReservation(anyLong(), anyLong(), anyLong()))
                .thenAnswer(invocation -> savedOrder);
    }

    @Test
    void redeemedPointsBecomeAnOrderDiscount() {
        when(loyaltyClient.quote(2L, 9L, 40L, "USD", null)).thenReturn(new BigDecimal("2.00"));

        var response = service.createPos(request(9L, 40L));

        assertEquals(new BigDecimal("10.00"), response.subtotal());
        assertEquals(new BigDecimal("2.00"), response.discount());
        assertEquals(new BigDecimal("8.00"), response.grandTotal());
        assertEquals(40L, response.loyaltyPointsRedeemed());
        assertEquals(DiscountMethod.LOYALTY, response.discounts().get(0).method());
        assertEquals("Redeemed 40 points", response.discounts().get(0).reason());
    }

    @Test
    void pointsNeedACustomerAndCannotExceedTheSale() {
        assertThrows(ResponseStatusException.class, () -> service.createPos(request(0L, 40L)));

        when(loyaltyClient.quote(2L, 9L, 400L, "USD", null)).thenReturn(new BigDecimal("20.00"));
        ResponseStatusException tooMuch = assertThrows(ResponseStatusException.class, () -> service.createPos(request(9L, 400L)));

        assertEquals("These points are worth more than the sale. Use fewer points.", tooMuch.getReason());
        verify(persistenceService, never()).save(any());
    }

    @Test
    void completionSettlesPointsOnceAndRecordsWhatWasEarned() {
        Order order = paidOrder();
        when(persistenceService.require(50L)).thenReturn(order);
        when(paymentClient.listForOrder(50L)).thenReturn(List.of(new PaymentSummaryResponse(
                1L, 50L, new BigDecimal("8.00"), BigDecimal.ZERO, "PAID")));
        when(persistenceService.markCompleted(50L)).thenAnswer(invocation -> {
            order.setOrderStatus(OrderStatus.COMPLETED);
            return order;
        });
        when(loyaltyClient.settle(50L, 2L, 9L, "POS-50", new BigDecimal("8.00"), "USD", null, 40L))
                .thenReturn(new LoyaltyClient.Settlement(40L, 8L, 88L));
        when(persistenceService.recordLoyaltyEarned(50L, 8L)).thenAnswer(invocation -> {
            order.setLoyaltyPointsEarned(8L);
            return order;
        });

        var response = service.complete(50L);
        service.complete(50L);

        assertEquals(8L, response.loyaltyPointsEarned());
        verify(loyaltyClient).settle(eq(50L), eq(2L), eq(9L), eq("POS-50"), any(), eq("USD"), any(), eq(40L));
    }

    @Test
    void loyaltyOutageDoesNotBlockTheSale() {
        Order order = paidOrder();
        when(persistenceService.require(50L)).thenReturn(order);
        when(paymentClient.listForOrder(50L)).thenReturn(List.of(new PaymentSummaryResponse(
                1L, 50L, new BigDecimal("8.00"), BigDecimal.ZERO, "PAID")));
        when(persistenceService.markCompleted(50L)).thenAnswer(invocation -> {
            order.setOrderStatus(OrderStatus.COMPLETED);
            return order;
        });
        when(loyaltyClient.settle(anyLong(), anyLong(), anyLong(), anyString(), any(), anyString(), any(), anyLong()))
                .thenThrow(new IllegalStateException("customer service is down"));

        var response = service.complete(50L);

        assertEquals(OrderStatus.COMPLETED, response.orderStatus());
        assertEquals(null, response.loyaltyPointsEarned());
    }

    private Order paidOrder() {
        Order order = Order.builder()
                .id(50L).orderNo("POS-50").businessId(2L).storeId(3L).customerId(9L)
                .orderChannel(OrderChannel.POS).orderStatus(OrderStatus.PENDING_PAYMENT).paymentStatus(PaymentStatus.UNPAID)
                .currencyCode("USD").grandTotal(new BigDecimal("8.00")).loyaltyPointsRedeemed(40L)
                .items(new ArrayList<>(List.of(OrderItem.builder().id(500L).variantId(11L).qty(4).reservationId(70L)
                        .unitPrice(new BigDecimal("2.50")).build())))
                .build();
        return order;
    }

    private static CreatePosOrderRequest request(Long customerId, Long points) {
        return new CreatePosOrderRequest(2L, 3L, customerId, null, 7L, "USD", null, null, null, null,
                BigDecimal.ZERO, null, "POS-key-1", List.of(new PosOrderItemRequest(11L, 4, null, null, null)),
                null, false, points);
    }
}
