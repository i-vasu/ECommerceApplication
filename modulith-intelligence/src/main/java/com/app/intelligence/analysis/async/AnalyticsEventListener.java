package com.app.intelligence.analysis.async;

import com.app.intelligence.analysis.services.AnalyticsService;
import com.app.core.events.ProductViewedEvent;
import com.app.core.events.ProductSearchEvent;
import com.app.core.events.AddToCartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for product activity events.
 * Asynchronously processes tracking logic to keep the main request threads
 * fast.
 */
@Component
public class AnalyticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListener.class);

    private final AnalyticsService analyticsService;

    public AnalyticsEventListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Async
    @EventListener
    public void onProductView(ProductViewedEvent event) {
        log.debug("Async Tracking: Product View for ID {}", event.productId());
        analyticsService.trackProductView(event.productId());
    }

    @Async
    @EventListener
    public void onProductSearch(ProductSearchEvent event) {
        log.debug("Async Tracking: Search for '{}' ({} results)", event.query(), event.resultCount());
        analyticsService.trackSearch(event.query(), event.resultCount());
    }

    @Async
    @EventListener
    public void onAddToCart(AddToCartEvent event) {
        log.debug("Async Tracking: Add to Cart for {}", event.itemCode());
        analyticsService.trackAddToCart(event.productId(), event.itemCode(), event.price());
    }
}
