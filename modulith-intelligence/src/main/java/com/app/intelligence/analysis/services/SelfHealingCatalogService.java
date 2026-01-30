package com.app.intelligence.analysis.services;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Autonomous Self-Healing Catalog & Inventory Re-balancing.
 * Monitors product health and adjusts visibility/ordering automatically.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SelfHealingCatalogService {

    private final ProductRepo productRepo;
    private final JdbcTemplate jdbcTemplate;
    private final com.app.core.services.FunnelAnalyticsService funnelService;

    /**
     * Periodically updates product quality scores based on returns and feedback.
     * Part of the "Self-Cleaning Catalog" logic.
     */
    @Scheduled(cron = "0 0 3 * * ?") // 3 AM
    @Transactional
    public void maintenance() {
        log.info("Starting Catalog Self-Healing Maintenance...");

        List<Product> products = productRepo.findAll();
        for (Product product : products) {
            updateQualityScore(product);
            rebalanceInventoryintent(product);
        }
    }

    private void updateQualityScore(Product product) {
        // Logic: Return Rate Impact
        String emailToReturnRateSql = "SELECT COUNT(*) FROM return_requests rr " +
                "JOIN order_items oi ON rr.item_code = oi.item_code " +
                "WHERE oi.product_id = ?";

        Long returns = jdbcTemplate.queryForObject(emailToReturnRateSql, Long.class, product.getProductId());

        String salesSql = "SELECT COUNT(*) FROM order_items WHERE product_id = ?";
        Long sales = jdbcTemplate.queryForObject(salesSql, Long.class, product.getProductId());

        double returnRate = (sales != null && sales > 0) ? (double) returns / sales : 0.0;

        // Base score 1.0. Deduct 2x return rate.
        double newScore = Math.max(0.1, 1.0 - (returnRate * 2.5));

        if (newScore < product.getQualityScore()) {
            log.warn("Deprioritizing Product {} (ID: {}) due to high return rate ({})",
                    product.getProductName(), product.getProductId(), returnRate);
        }

        product.setQualityScore(newScore);
        productRepo.save(product);
    }

    private void rebalanceInventoryintent(Product product) {
        // High Interest but Low Sales = Anomaly (maybe price is too high or reviews are
        // bad)
        String cartAddsSql = "SELECT COUNT(*) FROM activity_logs WHERE activity_type = 'CART_ADD' AND metadata->>'product_id' = ?";
        Long cartAdds = jdbcTemplate.queryForObject(cartAddsSql, Long.class, String.valueOf(product.getProductId()));

        String salesSql = "SELECT COUNT(*) FROM order_items WHERE product_id = ?";
        Long sales = jdbcTemplate.queryForObject(salesSql, Long.class, product.getProductId());

        if (cartAdds != null && cartAdds > 20 && (sales == null || sales < 2)) {
            log.info("Inventory Alert: Product {} (ID: {}) has high interest ({} adds) but zero/low sales. " +
                    "Triggering Autonomous Price Guard review.", product.getProductName(), product.getProductId(),
                    cartAdds);

            // Link to logic 3: Dynamic Pricing
            // For now, we tag it for review
            product.setQualityScore(product.getQualityScore() * 0.9); // Slight deprioritization until reviewed
            productRepo.save(product);
        }
    }
}
