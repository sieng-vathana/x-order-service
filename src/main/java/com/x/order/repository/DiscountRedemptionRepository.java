package com.x.order.repository;

import com.x.order.entity.DiscountRedemption;
import com.x.order.entity.DiscountRedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface DiscountRedemptionRepository extends JpaRepository<DiscountRedemption, Long> {

    long countByDiscountIdAndStatusIn(
            Long discountId, Collection<DiscountRedemptionStatus> statuses);

    long countByDiscountIdAndCustomerIdAndStatusIn(
            Long discountId, Long customerId, Collection<DiscountRedemptionStatus> statuses);

    Optional<DiscountRedemption> findByOrderId(Long orderId);
}
