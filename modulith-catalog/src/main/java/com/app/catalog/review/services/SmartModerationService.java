package com.app.catalog.review.services;

import com.app.catalog.review.entities.ProductReview;
import com.app.catalog.review.repositories.ProductReviewRepo;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.states.ReviewEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Smart Review Moderation Service.
 * Uses SpEL to automatically approve or flag user reviews.
 */
@Service
// @Log4j2
@RequiredArgsConstructor
public class SmartModerationService {
    private static final Logger log = LoggerFactory.getLogger(SmartModerationService.class);

    private final ProductReviewRepo reviewRepo;
    private final RuleEngineService ruleEngine;
    private final OperationalStateMachineService stateMachineService;

    public void moderateReview(ProductReview review) {
        log.info("Moderating review from user {} for product {}", review.getUserName(),
                review.getProduct().getProductId());

        Map<String, Object> context = new HashMap<>();
        context.put("review", review);
        context.put("isVerified", review.isVerifiedPurchase());

        // Logic:
        // 1. Auto-approve if rating is high AND it is a verified purchase.
        // 2. Flag if rating is low AND comment contains specific words (simulated).
        String approveRule = "review.rating >= 4 && isVerified";
        String flagRule = "review.rating <= 2 || review.comment.contains('fake')";

        if (ruleEngine.evaluate(approveRule, context)) {
            log.info("Smart Moderation: Auto-approving review {}", review.getReviewId());
            stateMachineService.triggerReviewEvent(review.getReviewId(), ReviewEvent.APPROVE);
            review.setApproved(true);
        } else if (ruleEngine.evaluate(flagRule, context)) {
            log.warn("Smart Moderation: Flagging review {} for manual audit", review.getReviewId());
            stateMachineService.triggerReviewEvent(review.getReviewId(), ReviewEvent.FLAG);
            review.setApproved(false);
        } else {
            // Default: Wait for manual
            review.setApproved(false);
        }

        reviewRepo.save(review);
    }
}
