package com.x.order.repository;

import com.x.order.entity.Discount;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByIdAndBusinessId(Long id, Long businessId);

    Page<Discount> findByBusinessId(Long businessId, Pageable pageable);

    boolean existsByBusinessIdAndCodeIgnoreCase(Long businessId, String code);

    boolean existsByBusinessIdAndCodeIgnoreCaseAndIdNot(Long businessId, String code, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Discount> findForUpdateByBusinessIdAndCodeIgnoreCase(Long businessId, String code);
}
