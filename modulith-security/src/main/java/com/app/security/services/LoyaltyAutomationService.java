package com.app.security.services;

import com.app.governance.rules.RuleEngineService;
import com.app.security.entities.User;
import com.app.security.repositories.UserRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Loyalty Tiering Service.
 * Automatically promotes users to different segments based on lifetime value (LTV).
 */
@Service
public class LoyaltyAutomationService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(LoyaltyAutomationService.class);

    private final UserRepo userRepo;
    private final RuleEngineService ruleEngine;

    public LoyaltyAutomationService(UserRepo userRepo, RuleEngineService ruleEngine) {
        this.userRepo = userRepo;
        this.ruleEngine = ruleEngine;
    }

    @Scheduled(cron = "0 0 3 * * ?") // Daily at 3 AM
    @Transactional
    public void updateLoyaltyTiers() {
        log.info("Starting Autonomous Loyalty Tiering processing...");
        List<User> users = userRepo.findAll();
        
        for (User user : users) {
             processTier(user);
        }
    }

    private void processTier(User user) {
        Map<String, Object> context = new HashMap<>();
        context.put("rewardPoints", user.getRewardPoints());
        
        // Dynamic Strategy: Upgrade to PLATINUM if points > 5000
        String platinumRule = "rewardPoints > 5000";
        String goldRule = "rewardPoints > 1000";

        if (ruleEngine.evaluate(platinumRule, context)) {
            if (!"PLATINUM".equals(user.getCustomerGroup())) {
                log.info("User {} promoted to PLATINUM tier!", user.getEmail());
                user.setCustomerGroup("PLATINUM");
                userRepo.save(user);
            }
        } else if (ruleEngine.evaluate(goldRule, context)) {
            if (!"GOLD".equals(user.getCustomerGroup())) {
                log.info("User {} promoted to GOLD tier!", user.getEmail());
                user.setCustomerGroup("GOLD");
                userRepo.save(user);
            }
        }
    }
}
