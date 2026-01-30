package com.app.governance.rules;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SystemRuleRepo extends JpaRepository<SystemRule, Long> {
    Optional<SystemRule> findByRuleKeyAndActiveTrue(String ruleKey);
}
