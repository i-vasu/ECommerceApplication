package com.app.catalog.repositories;

import com.app.catalog.entities.DynamicPricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DynamicPricingRuleRepo extends JpaRepository<DynamicPricingRule, Long> {

    @Query("SELECT r FROM DynamicPricingRule r WHERE r.active = true")
    List<DynamicPricingRule> findActiveRules();
}
