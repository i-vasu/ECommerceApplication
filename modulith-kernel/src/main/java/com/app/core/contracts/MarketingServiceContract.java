package com.app.core.contracts;

/**
 * Marketing Service Contract - Exposes marketing operations to other modules
 * This interface allows other modules to trigger marketing campaigns
 * without direct dependencies on the Marketing module.
 */
public interface MarketingServiceContract {

    /**
     * Handle order paid event for marketing campaigns
     */
    void handleOrderPaid(Long orderId);

    /**
     * Track user activity for personalization
     */
    void trackActivity(Long userId, String activityType, String details);
}
