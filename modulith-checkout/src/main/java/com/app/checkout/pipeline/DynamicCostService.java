package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
import com.app.governance.rules.RuleEngineService;
import com.app.security.entities.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Intelligent Tax & Shipping Engine powered by SpEL.
 * Allows calculating costs based on region, weight, and customer loyalty.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class DynamicCostService {

    private final RuleEngineService ruleEngine;

    public BigDecimal calculateShipping(CartContract cart, Address address) {
        Map<String, Object> context = new HashMap<>();
        context.put("cart", cart);
        context.put("address", address);
        context.put("total", cart.subTotal());
        
        // Dynamic Rule: Free shipping for orders > 500, otherwise flat 50
        String shippingRule = "total > 500 ? 0 : 50";
        
        Double cost = ruleEngine.evaluate(shippingRule, context, Double.class);
        return BigDecimal.valueOf(cost != null ? cost : 50.0);
    }

    public BigDecimal calculateTax(CartContract cart, Address address) {
        Map<String, Object> context = new HashMap<>();
        context.put("total", cart.subTotal());
        context.put("state", address.getState());

        // Dynamic Rule: 18% GST for most states, maybe lower for others
        String taxRule = "total * 0.18";
        
        Double tax = ruleEngine.evaluate(taxRule, context, Double.class);
        return BigDecimal.valueOf(tax != null ? tax : 0.0);
    }
}
