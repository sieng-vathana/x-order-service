package com.x.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // External IDs (Not mapped as JPA relationships to keep services decoupled)
    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "counter_id")
    private Long counterId;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "order_no", unique = true)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_channel", nullable = false, length = 16)
    private OrderChannel orderChannel;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 32)
    private OrderStatus orderStatus = OrderStatus.DRAFT;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 16)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "tax", precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "grand_total", precision = 10, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 32)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_type", length = 32)
    private FulfillmentType fulfillmentType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Loyalty points the customer spends on this order; deducted when it completes. */
    @Builder.Default
    @Column(name = "loyalty_points_redeemed", nullable = false, columnDefinition = "bigint not null default 0")
    private Long loyaltyPointsRedeemed = 0L;

    /** Points the order earned; null until loyalty is settled at completion. */
    @Column(name = "loyalty_points_earned")
    private Long loyaltyPointsEarned;

    @Version
    private Long version;

    // Relationships
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderCoupon> coupons = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDiscount> discounts = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeliveryAddress deliveryAddress;

    // Utility methods for bi-directional relationships
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addCoupon(OrderCoupon coupon) {
        coupons.add(coupon);
        coupon.setOrder(this);
    }

    public void addDiscount(OrderDiscount discount) {
        discounts.add(discount);
        discount.setOrder(this);
    }
}
