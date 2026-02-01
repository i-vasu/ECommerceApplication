package com.app.discovery.domain.listeners;

import com.app.core.events.ProductCreatedEvent;
import com.app.core.events.ProductEnrichedEvent;
import com.app.core.events.ProductSyncCompletedEvent;
import com.app.core.events.ProductUpdatedEvent;
import com.app.discovery.domain.services.AITaggingService;
import com.app.discovery.domain.services.VisualSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listeners for Discovery module.
 * Handles product indexing for search and visual discovery via events.
 * Decoupled: No direct dependency on Catalog repositories.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DiscoveryEventListener {

    private final VisualSearchService visualSearchService;
    private final AITaggingService aiTaggingService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handle product created - index for visual search and initiate AI tagging.
     */
    @Async
    @EventListener
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Discovery: Received ProductCreatedEvent for {}", event.itemCode());

        try {
            if (event.imageUrl() != null) {
                // Update visual search index
                visualSearchService.updateProductVector(event.productId(), event.imageUrl());

                // Generate AI tags and publish enrichment event
                enrichProductWithAI(event.productId(), event.itemCode(), event.imageUrl());

                log.info("Discovery: Successfully indexed product {}", event.itemCode());
            }
        } catch (Exception e) {
            log.error("Discovery: Failed to index product {} - {}", event.itemCode(), e.getMessage(), e);
        }
    }

    /**
     * Handle product updated - reindex for visual search.
     */
    @Async
    @EventListener
    public void handleProductUpdated(ProductUpdatedEvent event) {
        log.info("Discovery: Received ProductUpdatedEvent for {}", event.itemCode());

        try {
            if (event.imageUrl() != null) {
                // Update visual search index
                visualSearchService.updateProductVector(event.productId(), event.imageUrl());
                log.info("Discovery: Successfully reindexed product {}", event.itemCode());
            }
        } catch (Exception e) {
            log.error("Discovery: Failed to reindex product {} - {}", event.itemCode(), e.getMessage(), e);
        }
    }

    /**
     * Handle product sync completed - trigger full re-indexing or updates.
     */
    @Async
    @EventListener
    public void handleProductSyncCompleted(ProductSyncCompletedEvent event) {
        log.info("Discovery: Received ProductSyncCompletedEvent. Triggering resync for {} products.", event.syncedCount());
        // In a real scenario, this might trigger a batch re-index or update stats.
        // Currently, individual updates are handled via ProductCreated/Updated events.
        // This log serves as acknowledgment of the sync completion.
    }

    /**
     * Enrich product with AI-generated tags and publish event.
     */
    private void enrichProductWithAI(Long productId, String itemCode, String imageUrl) {
        try {
            log.debug("Discovery: Generating AI tags for product {}", itemCode);

            var aiTags = aiTaggingService.generateTags(imageUrl);
            if (!aiTags.isEmpty()) {
                log.info("Discovery: Publishing enrichment event for product {}: {}", itemCode, aiTags);

                eventPublisher.publishEvent(new ProductEnrichedEvent(
                        productId,
                        itemCode,
                        aiTags));
            }
        } catch (Exception e) {
            log.warn("Discovery: Failed to generate AI tags for {} - {}", itemCode, e.getMessage());
        }
    }
}
