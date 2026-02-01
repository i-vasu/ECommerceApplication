package com.app.order.services;

import com.app.governance.rules.RuleEngineService;
import com.app.order.entities.Vendor;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.VendorRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Vendor Settlement Service.
 * Calculates vendor payouts based on successful orders and dynamic commission
 * rules.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class VendorSettlementService {

    private final OrderRepo orderRepo;
    private final VendorRepo vendorRepo;
    private final RuleEngineService ruleEngine;
    private final com.app.governance.states.OperationalStateMachineService stateMachineService;

    @Scheduled(cron = "0 0 1 1 * ?") // Monthly on the 1st
    @Transactional
    public void processSettlements() {
        log.info("Starting Autonomous Monthly Vendor settlements...");
        List<Vendor> vendors = vendorRepo.findAll();

        for (Vendor vendor : vendors) {
            calculateSettlement(vendor);
            monitorVendorHealth(vendor);
        }
    }

    private void monitorVendorHealth(Vendor vendor) {
        // Logic: Return Rate analysis
        // Mocking return rate check
        double returnRate = 0.25; // 25% returns (High)

        Map<String, Object> context = new HashMap<>();
        context.put("returnRate", returnRate);

        // Rule: If return rate > 20%, flag for review
        if (ruleEngine.evaluate("returnRate > 0.2", context)) {
            log.warn("Vendor {} health check failed. Return rate: {}%. Triggering Governance Review.",
                    vendor.getName(), returnRate * 100);

            stateMachineService.triggerVendorEvent(vendor.getId(), com.app.governance.states.VendorEvent.SUSPEND);
        }
    }

    private void calculateSettlement(Vendor vendor) {
        // Logic: Calculate sum of all orders for this vendor in PAID state
        // (Mocking the vendor-order relationship for now)
        BigDecimal totalSales = BigDecimal.valueOf(50000);

        Map<String, Object> context = new HashMap<>();
        context.put("totalSales", totalSales);
        context.put("commissionRate", vendor.getCommissionRate());

        // SpEL Rule: Settlement = Sales * (1 - Commission)
        String settlementRule = "totalSales * (1 - commissionRate)";

        BigDecimal settlementAmount = ruleEngine.evaluate(settlementRule, context, BigDecimal.class);

        if (settlementAmount != null) {
            log.info("Settlement calculated for {}: {}", vendor.getName(), settlementAmount);
            vendor.setPendingSettlement(settlementAmount);
            vendorRepo.save(vendor);

            // Formalize the financial event in the state machine
            stateMachineService.triggerVendorEvent(vendor.getId(), com.app.governance.states.VendorEvent.APPROVE);

            // Trigger actual Bank Transfer (Simulated)
            log.info("Initiating bank transfer of {} to account {}", settlementAmount, vendor.getBankAccountNumber());
        }
    }
}
