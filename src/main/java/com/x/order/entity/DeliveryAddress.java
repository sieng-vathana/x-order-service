package com.x.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "delivery_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "receiver_name")
    private String receiverName;

    private String phone;

    @Column(name = "province_city", length = 100)
    private String provinceCity;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String commune;

    @Column(length = 100)
    private String village;

    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @Column(length = 255)
    private String landmark;

    @Column(name = "delivery_instructions", length = 500)
    private String deliveryInstructions;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
}
