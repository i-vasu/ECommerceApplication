package com.app.logistics.inventory.services;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.governance.rules.RuleEngineService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Inventory Optimization Service.
 * Handles Auto-Restock triggers and Liquidation pricing via SpEL.
 */
@Service
public class InventoryOptimizationService {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
            .getLogger(InventoryOptimizationService.class);

    private final ProductRepo productRepo;
    private final RuleEngineService ruleEngine;
    private final com.app.governance.states.OperationalStateMachineService stateMachineService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public InventoryOptimizationService(ProductRepo productRepo, RuleEngineService ruleEngine,
            com.app.governance.states.OperationalStateMachineService stateMachineService,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.productRepo = productRepo;
        this.ruleEngine = ruleEngine;
        this.stateMachineService = stateMachineService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void optimizeInventory() {
        log.info("Starting Autonomous Inventory Optimization...");
        List<Product> products = productRepo.findAll();

        for (Product product : products) {
            handleRestock(product);
            handleLiquidation(product);
        }
    }

    private void handleRestock(Product product) {
        Map<String, Object> context = new HashMap<>();
        context.put("stock", product.getQuantity());

        // SpEL Rule: Restock if stock falls below 10
        String restockRule = "stock < 10";
        int threshold = 10; // Added threshold variable

        if (ruleEngine.evaluate(restockRule, context)) {
            log.warn("Auto-Restock triggered for {}. Publishing restock event...", product.getProductName());

            // Formalize event in State Machine
            stateMachineService.triggerProductEvent(product.getProductId(),
                    com.app.governance.states.ProductEvent.RESTOCK);

            // Publish RestockRequestedEvent for ERPNext module to handle
            var event = new com.app.core.events.RestockRequestedEvent(
                    product.getProductId(),
                    product.getItemCode(),
                    50, // Standard restock quantity
                    product.getQuantity(),
                    threshold,
                    "Automatic restock triggered - stock below threshold");
            eventPublisher.publishEvent(event);

            log.info("RestockRequestedEvent published for product {} ({})",
                    product.getProductId(), product.getItemCode());
        }
    }

    private void handleLiquidation(Product product) {
        Map<String, Object> context = new HashMap<>();
        context.put("stock", product.getQuantity());
        context.put("createdAt", product.getCreatedAt());

        // SpEL Rule: Liquidate if stock > 100 AND product is older than 6 months
        String liquidationRule = "stock > 100 && createdAt.isBefore(T(java.time.LocalDateTime).now().minusMonths(6))";

        if (ruleEngine.evaluate(liquidationRule, context)) {
            log.info("Liquidation triggered for {}. Applying 30% discount...", product.getProductName());
            BigDecimal newPrice = product.getPrice().multiply(BigDecimal.valueOf(0.7));
            product.setSpecialPrice(newPrice);
            productRepo.save(product);
        }
    }
}
