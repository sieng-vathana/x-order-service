package com.x.order.service;

import com.x.order.entity.*;
import com.x.order.repository.DiscountRedemptionRepository;
import com.x.order.repository.DiscountRepository;
import com.x.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private DiscountRedemptionRepository redemptionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Captor
    private ArgumentCaptor<DiscountRedemption> redemptionCaptor;

    private DiscountService discountService;

    @BeforeEach
    void setUp() {
        discountService = new DiscountService(
                discountRepository, redemptionRepository, orderRepository);
    }

    @Test
    void appliesTargetedPercentageCodeAndReservesRedemption() {
        Discount discount = activeDiscount();
        discount.setType(DiscountType.PERCENTAGE);
        discount.setScope(DiscountScope.ITEM);
        discount.setValue(new BigDecimal("10.00"));
        discount.getVariantIds().add(11L);

        Order order = posOrder();
        order.addItem(orderItem(11L, new BigDecimal("50.00"), 2));

        when(discountRepository.findForUpdateByBusinessIdAndCodeIgnoreCase(1L, "SAVE10"))
                .thenReturn(Optional.of(discount));
        when(redemptionRepository.countByDiscountIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(0L);
        when(orderRepository.save(order)).thenReturn(order);

        Order result = discountService.applyCodeAndSave(
                order, "save10", new BigDecimal("10.00"));

        assertThat(result.getDiscount()).isEqualByComparingTo("10.00");
        assertThat(result.getTax()).isEqualByComparingTo("9.00");
        assertThat(result.getGrandTotal()).isEqualByComparingTo("99.00");
        assertThat(result.getItems().get(0).getDiscounts()).hasSize(1);
        assertThat(result.getItems().get(0).getDiscounts().get(0).getCode())
                .isEqualTo("SAVE10");

        verify(redemptionRepository).save(redemptionCaptor.capture());
        assertThat(redemptionCaptor.getValue().getStatus())
                .isEqualTo(DiscountRedemptionStatus.RESERVED);
        assertThat(redemptionCaptor.getValue().getOrder()).isSameAs(order);
    }

    @Test
    void rejectsCodeAfterGlobalUsageLimitIsReached() {
        Discount discount = activeDiscount();
        discount.setUsageLimit(1);
        Order order = posOrder();
        order.addItem(orderItem(11L, new BigDecimal("10.00"), 1));

        when(discountRepository.findForUpdateByBusinessIdAndCodeIgnoreCase(1L, "SAVE10"))
                .thenReturn(Optional.of(discount));
        when(redemptionRepository.countByDiscountIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(1L);

        assertThatThrownBy(() -> discountService.applyCodeAndSave(
                order, "SAVE10", BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("usage limit");

        verify(orderRepository, never()).save(any());
        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void allocatesOrderDiscountAcrossItemsBeforeCalculatingTax() {
        Discount discount = activeDiscount();
        Order order = posOrder();
        order.addItem(orderItem(11L, new BigDecimal("60.00"), 1));
        order.addItem(orderItem(12L, new BigDecimal("40.00"), 1));

        when(discountRepository.findForUpdateByBusinessIdAndCodeIgnoreCase(1L, "SAVE10"))
                .thenReturn(Optional.of(discount));
        when(redemptionRepository.countByDiscountIdAndStatusIn(eq(5L), anyCollection()))
                .thenReturn(0L);
        when(orderRepository.save(order)).thenReturn(order);

        discountService.applyCodeAndSave(order, "SAVE10", new BigDecimal("10.00"));

        BigDecimal allocated = order.getItems().stream()
                .map(OrderItem::getOrderDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal itemTotals = order.getItems().stream()
                .map(OrderItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(allocated).isEqualByComparingTo("10.00");
        assertThat(order.getTax()).isEqualByComparingTo("9.00");
        assertThat(itemTotals).isEqualByComparingTo(order.getGrandTotal());
    }

    @Test
    void releasesReservedRedemptionWhenOrderIsCancelled() {
        DiscountRedemption redemption = DiscountRedemption.builder()
                .status(DiscountRedemptionStatus.RESERVED)
                .build();
        when(redemptionRepository.findByOrderId(20L)).thenReturn(Optional.of(redemption));

        discountService.release(20L);

        assertThat(redemption.getStatus()).isEqualTo(DiscountRedemptionStatus.RELEASED);
        verify(redemptionRepository).save(redemption);
    }

    private Discount activeDiscount() {
        return Discount.builder()
                .id(5L)
                .businessId(1L)
                .name("Save ten")
                .code("SAVE10")
                .method(DiscountMethod.CODE)
                .type(DiscountType.PERCENTAGE)
                .scope(DiscountScope.ORDER)
                .channel(DiscountChannel.BOTH)
                .value(new BigDecimal("10.00"))
                .status(DiscountStatus.ACTIVE)
                .storeIds(new HashSet<>())
                .variantIds(new HashSet<>())
                .build();
    }

    private Order posOrder() {
        return Order.builder()
                .id(20L)
                .businessId(1L)
                .storeId(2L)
                .customerId(3L)
                .orderChannel(OrderChannel.POS)
                .currencyCode("USD")
                .subtotal(new BigDecimal("100.00"))
                .discount(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("100.00"))
                .items(new ArrayList<>())
                .coupons(new ArrayList<>())
                .build();
    }

    private OrderItem orderItem(Long variantId, BigDecimal unitPrice, int quantity) {
        return OrderItem.builder()
                .variantId(variantId)
                .unitPrice(unitPrice)
                .qty(quantity)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .total(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }
}
