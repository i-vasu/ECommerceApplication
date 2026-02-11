package com.app.checkout.domain;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.core.contracts.CartContract.CartItemContract;
import com.app.governance.rules.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Price Integrity & Margin Guard.
 * Prevents "Price Leaks" or accidental losses due to bad promotion logic.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class PriceGuardService {

    private final RuleEngineService ruleEngine;
    private final ProductRepo productRepo;

    /**
     * Validates that the cart items don't violate minimum margin policies.
     * 
     * @return true if price is safe, false if it's a potential anomaly.
     */
    public boolean isPriceSafe(List<CartItemContract> items) {
        log.info("PriceGuard: Analyzing margin for {} items", items.size());

        for (CartItemContract item : items) {
            Product p = productRepo.findById(item.productId()).orElse(null);
            if (p == null)
                continue;

            Map<String, Object> context = new HashMap<>();
            context.put("soldPrice", item.price());
            context.put("baseCost", p.getPrice()); // Assuming base price is cost

            // Rule: Margin must be at least 70% of base price (allowing for 30% discount max)
            // This is a dynamic safeguard.
            String marginRule = "soldPrice >= (baseCost * 0.7)";

            if (!ruleEngine.evaluate(marginRule, context)) {
                log.error(
                        "CRITICAL: Price Guard Violation! Item {} sold at {}, but cost is {}. Difference exceeds safety threshold.",
                        item.productName(), item.price(), p.getPrice());
                return false;
            }
        }

        return true;
    }
}
