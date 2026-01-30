package com.app.finance.promo.repositories;

import com.app.finance.promo.entities.PromotionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRuleRepo extends JpaRepository<PromotionRule, Long> {

    @Query("SELECT r FROM PromotionRule r WHERE r.active = true AND r.couponCode = :code ORDER BY r.priority DESC")
    List<PromotionRule> findActiveRulesByCoupon(String code);

    @Query("SELECT r FROM PromotionRule r WHERE r.active = true AND r.couponCode IS NULL ORDER BY r.priority DESC")
    List<PromotionRule> findActiveGlobalRules();
}
