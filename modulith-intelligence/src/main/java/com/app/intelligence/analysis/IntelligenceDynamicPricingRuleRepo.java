package com.app.intelligence.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntelligenceDynamicPricingRuleRepo extends JpaRepository<IntelligenceDynamicPricingRule, Long> {
 
    @Query("SELECT r FROM IntelligenceDynamicPricingRule r WHERE r.active = true ORDER BY r.priority DESC")
    List<IntelligenceDynamicPricingRule> findActiveRules();
}
