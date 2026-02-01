package com.app.finance.promo.repositories;

import com.app.finance.promo.entities.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepo extends JpaRepository<CouponUsage, Long> {

    List<CouponUsage> findByUserId(Long userId);

    List<CouponUsage> findByCouponCouponId(Long couponId);

    boolean existsByCouponCouponIdAndUserId(Long couponId, Long userId);
}
