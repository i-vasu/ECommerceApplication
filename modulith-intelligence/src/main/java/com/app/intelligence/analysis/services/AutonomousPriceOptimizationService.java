package com.app.intelligence.analysis.services;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Autonomous Price Guard & Dynamic Pricing Engine.
 * Adjusts prices based on velocity (demand) and inventory levels.
 */
@Service("autonomousPriceOptimizationService")
@RequiredArgsConstructor
public class AutonomousPriceOptimizationService {
 
    private static final Logger log = LoggerFactory.getLogger(AutonomousPriceOptimizationService.class);

    private final ProductRepo productRepo;
    private final StringRedisTemplate redisTemplate;

    private static final String TRENDING_KEY_PREFIX = "analytics:trending:daily:";

    @Scheduled(cron = "0 0 4 * * ?") // 4 AM
    @Transactional
    public void runPriceOptimization() {
        log.info("Starting Autonomous Price Optimization...");

        List<Product> products = productRepo.findAll();
        String todayKey = TRENDING_KEY_PREFIX
                + java.time.LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ISO_DATE);

        for (Product product : products) {
            optimizePrice(product, todayKey);
        }
    }

    private void optimizePrice(Product product, String analyticsKey) {
        Double views = redisTemplate.opsForZSet().score(analyticsKey, String.valueOf(product.getProductId()));
        if (views == null)
            views = 0.0;

        int stock = product.getQuantity();
        BigDecimal currentPrice = product.getSpecialPrice();
        BigDecimal originalPrice = product.getPrice();

        // 1. SURGE: High demand (views > 100) and Low Stock (< 10)
        if (views > 100 && stock < 10) {
            BigDecimal surgePrice = currentPrice.multiply(BigDecimal.valueOf(1.10)).setScale(2, RoundingMode.HALF_UP);
            // Cap surge at original price + 20%
            BigDecimal maxPrice = originalPrice.multiply(BigDecimal.valueOf(1.20));
            if (surgePrice.compareTo(maxPrice) < 0) {
                log.info("SURGE: Increasing price for {} due to high demand and low stock. {} -> {}",
                        product.getProductName(), currentPrice, surgePrice);
                product.setSpecialPrice(surgePrice);
            }
        }

        // 2. CLEARANCE: Low demand (views < 5) and High Stock (> 50)
        else if (views < 5 && stock > 50) {
            BigDecimal clearancePrice = currentPrice.multiply(BigDecimal.valueOf(0.90)).setScale(2,
                    RoundingMode.HALF_UP);
            // Min price: 50% of original
            BigDecimal minPrice = originalPrice.multiply(BigDecimal.valueOf(0.50));
            if (clearancePrice.compareTo(minPrice) > 0) {
                log.info("CLEARANCE: Dropping price for {} due to low demand and overstock. {} -> {}",
                        product.getProductName(), currentPrice, clearancePrice);
                product.setSpecialPrice(clearancePrice);
            }
        }

        productRepo.save(product);
    }
}
