package com.app.intelligence.analysis;

import com.app.intelligence.analysis.DynamicPricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicPricingRuleRepo extends JpaRepository<DynamicPricingRule, Long> {

    @Query("SELECT r FROM DynamicPricingRule r WHERE r.active = true ORDER BY r.priority DESC")
    List<DynamicPricingRule> findActiveRules();
}
