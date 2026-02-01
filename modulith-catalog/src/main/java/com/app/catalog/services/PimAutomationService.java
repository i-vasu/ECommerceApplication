package com.app.catalog.services;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.states.PimEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Autonomous Product Information Management (PIM) Service.
 * Automates the enrichment, tagging, and publication of products.
 */
@Service
// @Log4j2
@RequiredArgsConstructor
public class PimAutomationService {

    private static final Logger log = LoggerFactory.getLogger(PimAutomationService.class);

    private final ProductRepo productRepo;
    private final OperationalStateMachineService stateMachineService;
    private final com.app.governance.rules.RuleEngineService ruleEngine;

    @Async
    public void processNewProduct(Product product) {
        log.info("Starting Autonomous PIM flow for product: {}", product.getProductName());

        // 1. Trigger AI Tagging
        stateMachineService.triggerPimEvent(product.getProductId(), PimEvent.SUBMIT);

        // Simulate AI Tagging
        simulateTagging(product);
        stateMachineService.triggerPimEvent(product.getProductId(), PimEvent.TAG_COMPLETE);

        // 2. Automate Enrichment
        simulateEnrichment(product);
        stateMachineService.triggerPimEvent(product.getProductId(), PimEvent.ENRICH_COMPLETE);

        // 3. Final Publication Decision via SpEL
        Map<String, Object> context = new java.util.HashMap<>();
        context.put("p", product);

        // Rule: A product can go live if it has a price and a description > 50 chars
        String goLiveRule = "p.price != null && p.description.length() > 50";

        if (ruleEngine.evaluate(goLiveRule, context)) {
            log.info("PIM: Product {} passed quality check. Publishing...", product.getProductName());
            stateMachineService.triggerPimEvent(product.getProductId(), PimEvent.PUBLISH);
        } else {
            log.warn("PIM: Product {} failed quality check. Requires manual intervention.", product.getProductName());
        }
    }

    private void simulateTagging(Product product) {
        product.setTags(List.of("AI_GENERATED", "NEW_ARRIVAL"));
        productRepo.save(product);
    }

    private void simulateEnrichment(Product product) {
        // Enrichment logic
    }
}
