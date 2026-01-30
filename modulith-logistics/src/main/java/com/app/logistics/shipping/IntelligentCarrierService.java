package com.app.logistics.shipping;

import com.app.governance.rules.RuleEngineService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Intelligent Carrier Routing Service.
 * Uses SpEL rules to determine the optimal shipping provider.
 */
@Service
public class IntelligentCarrierService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
            .getLogger(IntelligentCarrierService.class);

    private final RuleEngineService ruleEngine;

    public IntelligentCarrierService(RuleEngineService ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * Determines the optimal shipping provider.
     * Note: Taking generic Object to avoid direct dependency on Order entity.
     */
    public ShipmentServiceImpl.ShippingProvider getOptimalProvider(Object order, double totalAmount) {
        log.info("Evaluating optimal carrier for Order Context");

        Map<String, Object> context = new HashMap<>();
        context.put("totalAmount", totalAmount);
        context.put("pincode", "560001"); // Mocking pincode for logic

        // Dynamic Rule: If order is high value (> ₹10k), use Shiprocket.
        // If hyperlocal (e.g., Bangalore pincode starts with 56), use Shadowfax.
        String carrierRule = "totalAmount > 10000 ? 'SHIPROCKET' : (pincode.startsWith('56') ? 'SHADOWFAX' : 'SHIPROCKET')";

        String provider = ruleEngine.evaluate(carrierRule, context, String.class);
        log.info("Rule Engine selected provider: {}", provider);

        try {
            return ShipmentServiceImpl.ShippingProvider.valueOf(provider != null ? provider : "SHIPROCKET");
        } catch (Exception e) {
            return ShipmentServiceImpl.ShippingProvider.SHIPROCKET;
        }
    }
}
