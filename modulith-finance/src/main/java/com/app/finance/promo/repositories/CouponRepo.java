package com.app.finance.promo.repositories;

import com.app.finance.promo.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepo extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.validFrom <= :now AND c.validTo >= :now")
    List<Coupon> findAllActiveCoupons(LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.active = true AND c.validFrom <= :now AND c.validTo >= :now")
    Optional<Coupon> findActiveByCode(String code, LocalDateTime now);
}
