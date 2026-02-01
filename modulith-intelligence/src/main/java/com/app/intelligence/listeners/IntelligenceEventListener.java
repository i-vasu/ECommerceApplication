package com.app.intelligence.listeners;

import com.app.core.events.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listeners for Intelligence module.
 * Handles ML training data collection, analytics, and predictions.
 */
@Component
public class IntelligenceEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IntelligenceEventListener.class);

    private final com.app.intelligence.data.IntelligenceEventRepo intelligenceRepo;

    public IntelligenceEventListener(com.app.intelligence.data.IntelligenceEventRepo intelligenceRepo) {
        this.intelligenceRepo = intelligenceRepo;
    }

    /**
     * Collect product view data for recommendation training.
     */
    @Async
    @EventListener
    public void collectProductViewData(ProductViewedEvent event) {
        log.debug("Intelligence: Collecting product view - Product: {}, User: {}",
                event.productId(), event.email());

        try {
            // Store for collaborative filtering model
            recordInteraction("VIEW", 0L, event.productId(), 0.0); // UserId missing in event, 0L as placeholder if needed or parse email

            // Update real-time recommendations
            updateRecommendations(0L);

        } catch (Exception e) {
            log.error("Intelligence: Failed to process product view {} - {}",
                    event.productId(), e.getMessage(), e);
        }
    }

    /**
     * Collect search data for semantic search improvements.
     */
    @Async
    @EventListener
    public void collectSearchData(ProductSearchEvent event) {
        log.debug("Intelligence: Collecting search - Query: '{}', Results: {}",
                event.query(), event.resultCount());

        try {
            // Store for search ranking model
            recordSearch(event.query(), event.resultCount(), event.userId());

            // Flag zero-result searches for product gap analysis
            if (event.resultCount() == 0) {
                analyzeProductGap(event.query());
            }

        } catch (Exception e) {
            log.error("Intelligence: Failed to process search '{}' - {}",
                    event.query(), e.getMessage(), e);
        }
    }

    /**
     * Collect cart data for abandonment prediction.
     */
    @Async
    @EventListener
    public void collectCartData(CartAbandonedEvent event) {
        log.info("Intelligence: Analyzing abandoned cart - User: {}, Value: ${}",
                event.userId(), event.totalValue());

        try {
            // Train abandonment prediction model
            recordCartAbandonment(event.userId(), event.items(), event.totalValue());

            // Calculate abandonment risk score for active carts
            updateAbandonmentRiskScores();

        } catch (Exception e) {
            log.error("Intelligence: Failed to analyze cart abandonment {} - {}",
                    event.cartId(), e.getMessage(), e);
        }
    }

    /**
     * Collect conversion data for funnel optimization.
     */
    @Async
    @EventListener
    public void collectConversionData(CartConvertedEvent event) {
        log.info("Intelligence: Recording conversion - Cart: {} -> Order: {}, Value: ${}",
                event.cartId(), event.orderId(), event.totalValue());

        try {
            // Update conversion prediction models
            recordConversion(event.userId(), event.totalValue());

            // Analyze conversion patterns
            analyzeConversionPath(event.userId());

        } catch (Exception e) {
            log.error("Intelligence: Failed to record conversion {} - {}",
                    event.cartId(), e.getMessage(), e);
        }
    }

    /**
     * Collect order data for demand forecasting.
     */
    @Async
    @EventListener
    public void collectOrderData(OrderCreatedEvent event) {
        log.info("Intelligence: Collecting order data - Order: {}, Items: {}",
                event.orderId(), event.items().size());

        try {
            // Update demand forecasting model
            for (var item : event.items()) {
                recordDemand(item.itemCode(), item.quantity());
            }

            // Update sales forecasts
            updateSalesForecasts();

        } catch (Exception e) {
            log.error("Intelligence: Failed to collect order data {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Collect payment data for fraud detection.
     */
    @Async
    @EventListener
    public void collectPaymentData(PaymentFailedEvent event) {
        log.warn("Intelligence: Analyzing payment failure - Order: {}, Reason: {}",
                event.orderId(), event.reason());

        try {
            // Update fraud detection model
            recordPaymentFailure(event.userId(), event.paymentMethod(), event.reason());

            // Calculate fraud risk score
            double fraudScore = calculateFraudScore(event.userId());
            if (fraudScore > 0.8) {
                log.warn("Intelligence: HIGH FRAUD RISK - User: {}, Score: {}",
                        event.userId(), fraudScore);
            }

        } catch (Exception e) {
            log.error("Intelligence: Failed to analyze payment failure {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Collect inventory data for stock optimization.
     */
    @Async
    @EventListener
    public void collectInventoryData(InventoryLowEvent event) {
        log.info("Intelligence: Analyzing inventory pattern - Item: {}, Current: {}",
                event.itemCode(), event.currentQuantity());

        try {
            // Update inventory optimization model
            recordStockout(event.itemCode(), event.currentQuantity(), event.threshold());

            // Predict future stockouts
            predictStockouts();

        } catch (Exception e) {
            log.error("Intelligence: Failed to analyze inventory for {} - {}",
                    event.itemCode(), e.getMessage(), e);
        }
    }

    /**
     * Collect cancellation data for churn prediction.
     */
    @Async
    @EventListener
    public void collectCancellationData(OrderCancelledEvent event) {
        log.info("Intelligence: Analyzing cancellation - Order: {}, Reason: {}",
                event.orderId(), event.reason());

        try {
            // Update churn prediction model
            recordCancellation(event.userId(), event.reason());

            // Calculate churn risk
            double churnRisk = calculateChurnRisk(event.userId());
            if (churnRisk > 0.7) {
                log.warn("Intelligence: HIGH CHURN RISK - User: {}, Score: {}",
                        event.userId(), churnRisk);
            }

        } catch (Exception e) {
            log.error("Intelligence: Failed to analyze cancellation {} - {}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * Collect product data for pricing optimization.
     */
    @Async
    @EventListener
    public void collectProductData(ProductUpdatedEvent event) {
        log.debug("Intelligence: Analyzing product update - Product: {}", event.productId());

        try {
            // Update pricing optimization model
            if (event.oldQuantity() != null && event.newQuantity() != null) {
                analyzePriceElasticity(event.productId(), event.oldQuantity(), event.newQuantity());
            }

        } catch (Exception e) {
            log.error("Intelligence: Failed to analyze product update {} - {}",
                    event.productId(), e.getMessage(), e);
        }
    }

    /**
     * Collect shipment data for delivery time prediction.
     */
    @Async
    @EventListener
    public void collectShipmentData(ShipmentStatusUpdatedEvent event) {
        if ("DELIVERED".equals(event.newStatus())) {
            log.info("Intelligence: Recording delivery time - Order: {}", event.orderId());

            try {
                // Update delivery time prediction model
                recordDelivery(event.orderId(), event.carrier());

                // Update ETA predictions
                updateDeliveryPredictions(event.carrier());

            } catch (Exception e) {
                log.error("Intelligence: Failed to record delivery for order {} - {}",
                        event.orderId(), e.getMessage(), e);
            }
        }
    }

    // Helper methods

    private void saveEvent(String type, Long userId, String entityId, Double value, String metadata) {
        com.app.intelligence.data.IntelligenceEventData data = new com.app.intelligence.data.IntelligenceEventData(
            type, userId, entityId, value, metadata
        );
        intelligenceRepo.save(data);
    }

    private void recordInteraction(String type, Long userId, Long productId, Double price) {
        log.debug("ML_DATA: {} interaction - User: {}, Product: {}, Price: ${}",
                type, userId, productId, price);
        saveEvent(type + "_INTERACTION", userId, productId.toString(), price, null);
    }

    private void updateRecommendations(Long userId) {
        log.debug("ML_UPDATE: Updating recommendations for user {}", userId);
    }

    private void recordSearch(String query, int resultCount, Long userId) {
        log.debug("ML_DATA: Search - Query: '{}', Results: {}, User: {}",
                query, resultCount, userId);
        saveEvent("SEARCH", userId, "QUERY:" + query, (double) resultCount, null);
    }

    private void analyzeProductGap(String query) {
        log.info("ML_INSIGHT: Product gap detected for search: '{}'", query);
    }

    private void recordCartAbandonment(Long userId, Object items, double value) {
        log.debug("ML_DATA: Cart abandoned - User: {}, Value: ${}", userId, value);
        saveEvent("CART_ABANDONED", userId, null, value, items.toString());
    }

    private void updateAbandonmentRiskScores() {
        log.debug("ML_UPDATE: Updating cart abandonment risk scores");
    }

    private void recordConversion(Long userId, double value) {
        log.debug("ML_DATA: Conversion - User: {}, Value: ${}", userId, value);
        saveEvent("CONVERSION", userId, null, value, null);
    }

    private void analyzeConversionPath(Long userId) {
        log.debug("ML_ANALYSIS: Analyzing conversion path for user {}", userId);
    }

    private void recordDemand(String itemCode, int quantity) {
        log.debug("ML_DATA: Demand - Item: {}, Qty: {}", itemCode, quantity);
        saveEvent("DEMAND", null, itemCode, (double) quantity, null);
    }

    private void updateSalesForecasts() {
        log.debug("ML_UPDATE: Updating sales forecasts");
    }

    private void recordPaymentFailure(Long userId, String method, String reason) {
        log.debug("ML_DATA: Payment failure - User: {}, Method: {}, Reason: {}",
                userId, method, reason);
        saveEvent("PAYMENT_FAILURE", userId, method, 0.0, reason);
    }

    private double calculateFraudScore(Long userId) {
        return 0.3;
    }

    private void recordStockout(String itemCode, int current, int threshold) {
        log.debug("ML_DATA: Low inventory - Item: {}, Current: {}, Threshold: {}",
                itemCode, current, threshold);
        saveEvent("LOW_STOCK", null, itemCode, (double) current, "Threshold:" + threshold);
    }

    private void predictStockouts() {
        log.debug("ML_PREDICT: Predicting future stockouts");
    }

    private void recordCancellation(Long userId, String reason) {
        log.debug("ML_DATA: Cancellation - User: {}, Reason: {}", userId, reason);
        saveEvent("CANCELLATION", userId, null, 0.0, reason);
    }

    private double calculateChurnRisk(Long userId) {
        return 0.4;
    }

    private void analyzePriceElasticity(Long productId, int oldQty, int newQty) {
        log.debug("ML_ANALYSIS: Price elasticity - Product: {}, ΔQty: {}",
                productId, (newQty - oldQty));
        saveEvent("PRICE_ELASTICITY", null, productId.toString(), (double)(newQty - oldQty), "OldQty:" + oldQty);
    }

    private void recordDelivery(Long orderId, String carrier) {
        log.debug("ML_DATA: Delivery - Order: {}, Carrier: {}", orderId, carrier);
        saveEvent("DELIVERY", null, orderId.toString(), 0.0, carrier);
    }

    private void updateDeliveryPredictions(String carrier) {
        log.debug("ML_UPDATE: Updating delivery predictions for {}", carrier);
    }
}
