package com.x.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "discounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_discounts_business_code",
                columnNames = {"business_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiscountMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiscountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiscountScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiscountChannel channel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "minimum_order_amount", precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(name = "maximum_discount_amount", precision = 12, scale = 2)
    private BigDecimal maximumDiscountAmount;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_limit_per_customer")
    private Integer usageLimitPerCustomer;

    @Column(nullable = false)
    private boolean combinable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiscountStatus status;

    @ElementCollection
    @CollectionTable(
            name = "discount_stores",
            joinColumns = @JoinColumn(name = "discount_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_discount_stores_discount_store",
                    columnNames = {"discount_id", "store_id"}))
    @Column(name = "store_id", nullable = false)
    @Builder.Default
    private Set<Long> storeIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "discount_products",
            joinColumns = @JoinColumn(name = "discount_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_discount_products_discount_product",
                    columnNames = {"discount_id", "product_id"}))
    @Column(name = "product_id", nullable = false)
    @Builder.Default
    private Set<Long> productIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "discount_variants",
            joinColumns = @JoinColumn(name = "discount_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_discount_variants_discount_variant",
                    columnNames = {"discount_id", "variant_id"}))
    @Column(name = "variant_id", nullable = false)
    @Builder.Default
    private Set<Long> variantIds = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
