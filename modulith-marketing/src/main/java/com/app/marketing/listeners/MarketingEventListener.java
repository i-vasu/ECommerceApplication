package com.app.marketing.listeners;

import com.app.core.events.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listeners for Marketing module.
 * Handles cart abandonment, payment failures, order creation, and product
 * views.
 */
@Component
public class MarketingEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MarketingEventListener.class);

    private final com.app.marketing.services.EmailService emailService;
    private final com.app.marketing.services.MarketingService marketingService;

    public MarketingEventListener(com.app.marketing.services.EmailService emailService,
            com.app.marketing.services.MarketingService marketingService) {
        this.emailService = emailService;
        this.marketingService = marketingService;
    }

    /**
     * Handle cart abandonment - send reminder email.
     */
    @Async
    @EventListener
    public void handleCartAbandoned(CartAbandonedEvent event) {
        log.info("Marketing: Processing abandoned cart for user {} - Cart value: ${}",
                event.userId(), event.totalValue());

        try {
            // TODO: Send abandoned cart reminder email
            log.info("Marketing: Would send abandoned cart email to {} with {} items (total: ${})",
                    event.email(), event.items().size(), event.totalValue());

            // Schedule follow-up emails
            scheduleAbandonedCartCampaign(event);

        } catch (Exception e) {
            log.error("Marketing: Failed to process abandoned cart for user {} - {}",
                    event.userId(), e.getMessage(), e);
        }
    }

    /**
     * Handle payment failure - send retry email.
     */
    @Async
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Marketing: Processing payment failure for order {} - Reason: {}",
                event.orderId(), event.reason());

        try {
            // TODO: Send payment retry email
            log.info("Marketing: Would send payment retry email for order {} (amount: ${})",
                    event.orderId(), event.amount());

            // Track payment failure for analytics
            trackPaymentFailure(event);

        } catch (Exception e) {
            log.error("Marketing: Failed to process payment failure for order {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle order creation - send confirmation email.
     */
    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Marketing: Sending order confirmation for order {} to user {}",
                event.orderId(), event.userId());

        try {
            emailService.sendOrderConfirmation(
                    event.email(),
                    event.orderId(),
                    event.totalAmount(),
                    "INTERNAL_" + event.orderId()); // Fallback PG ID

            // Track conversion
            trackOrderConversion(event);

        } catch (Exception e) {
            log.error("Marketing: Failed to send order confirmation for order {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle cart conversion - track in analytics.
     */
    @Async
    @EventListener
    public void handleCartConverted(CartConvertedEvent event) {
        log.info("Marketing: Cart {} converted to order {} - Value: ${}",
                event.cartId(), event.orderId(), event.totalValue());

        try {
            // Track conversion funnel
            log.info("Marketing: Tracking conversion - Cart {} -> Order {} (${}) for user {}",
                    event.cartId(), event.orderId(), event.totalValue(), event.userId());

            // Update customer lifetime value
            updateCustomerMetrics(event.userId(), event.totalValue());

        } catch (Exception e) {
            log.error("Marketing: Failed to track cart conversion {} - {}",
                    event.cartId(), e.getMessage(), e);
        }
    }

    /**
     * Handle product viewed - track for recommendations.
     */
    @Async
    @EventListener
    public void handleProductViewed(ProductViewedEvent event) {
        log.debug("Marketing: Product {} viewed by user {}", event.productId(), event.email());

        try {
            // Track for personalization
            trackProductView(event);

            // Trigger retargeting if appropriate
            if (shouldTriggerRetargeting(event)) {
                scheduleRetargetingCampaign(event);
            }

        } catch (Exception e) {
            log.error("Marketing: Failed to track product view {} - {}",
                    event.productId(), e.getMessage(), e);
        }
    }

    /**
     * Handle product search - track search terms.
     */
    @Async
    @EventListener
    public void handleProductSearch(ProductSearchEvent event) {
        log.debug("Marketing: Search performed: '{}' by user {}", event.query(), event.userId());

        try {
            // Track popular searches
            trackSearchTerm(event.query());

            // Detect zero-result searches for product ideas
            if (event.resultCount() == 0) {
                log.warn("Marketing: Zero results for search: '{}' - potential product gap",
                        event.query());
            }

        } catch (Exception e) {
            log.error("Marketing: Failed to track search '{}' - {}",
                    event.query(), e.getMessage(), e);
        }
    }

    /**
     * Handle shipment delivered - request review.
     */
    @Async
    @EventListener
    public void handleShipmentDelivered(ShipmentStatusUpdatedEvent event) {
        if ("DELIVERED".equals(event.newStatus())) {
            log.info("Marketing: Order {} delivered - scheduling review request", event.orderId());

            try {
                // TODO: Send review request email (after 3 days)
                log.info("Marketing: Would schedule review request for order {}", event.orderId());

            } catch (Exception e) {
                log.error("Marketing: Failed to schedule review request for order {} - {}",
                        event.orderId(), e.getMessage(), e);
            }
        }
    }

    // Helper methods (to be implemented with actual services)

    private void scheduleAbandonedCartCampaign(CartAbandonedEvent event) {
        log.debug("Scheduling abandoned cart campaign: 1hr, 24hr, 7days");
    }

    private void trackPaymentFailure(PaymentFailedEvent event) {
        log.debug("Tracking payment failure: method={}, reason={}",
                event.paymentMethod(), event.reason());
    }

    private void trackOrderConversion(OrderCreatedEvent event) {
        log.debug("Tracking order conversion: order={}, amount=${}",
                event.orderId(), event.totalAmount());
    }

    private void updateCustomerMetrics(Long userId, double orderValue) {
        log.debug("Updating customer {} metrics: +${}", userId, orderValue);
    }

    private void trackProductView(ProductViewedEvent event) {
        log.debug("Tracking product view: product={}, user={}",
                event.productId(), event.email());
    }

    private boolean shouldTriggerRetargeting(ProductViewedEvent event) {
        // Trigger retargeting for high-value products
        return event.price() != null && event.price().doubleValue() > 1000.0;
    }

    private void scheduleRetargetingCampaign(ProductViewedEvent event) {
        log.debug("Scheduling retargeting campaign for product {}", event.productId());
    }

    private void trackSearchTerm(String query) {
        log.debug("Tracking search term: '{}'", query);
    }
}
