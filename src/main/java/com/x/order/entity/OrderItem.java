package com.x.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // External Reference to Product Variant
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "variant_name", length = 160)
    private String variantName;

    @Column(length = 100)
    private String sku;

    @Column(length = 100)
    private String barcode;

    private Integer qty;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 16)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "order_discount", precision = 10, scale = 2)
    private BigDecimal orderDiscountAmount;

    @Column(name = "tax", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemDiscount> discounts = new ArrayList<>();

    public void addDiscount(OrderItemDiscount discount) {
        discounts.add(discount);
        discount.setOrderItem(this);
    }
}
