package com.x.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // External Reference to Coupon in Promotion Service
    @Column(name = "coupon_id")
    private Long couponId;
}
