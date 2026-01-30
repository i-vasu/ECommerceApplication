package com.app.order.listeners;

import com.app.core.events.ReturnRequestedEvent;
import com.app.core.events.ReturnValidatedEvent;
import com.app.core.events.ReturnValidatedEvent.ValidatedReturnItem;
import com.app.core.events.ReturnApprovedEvent;
import com.app.order.entities.Order;
import com.app.order.entities.OrderItem;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.core.APIException;
import com.app.governance.states.OrderStatus;
import com.app.governance.rules.RuleEngineService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Log4j2
@RequiredArgsConstructor
public class OrderReturnListener {

    private final OrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final RuleEngineService ruleEngine;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Listen for return requests and validate them using Order module rules.
     */
    @EventListener
    public void handleReturnRequested(ReturnRequestedEvent event) {
        log.info("Order: Validating return request for order {}", event.orderId());

        try {
            Order order = orderRepo.findById(event.orderId())
                    .orElseThrow(() -> new APIException("Order not found: " + event.orderId()));

            if (!order.getEmail().equals(event.email())) {
                throw new APIException("Unauthorized return request");
            }

            if (order.getOrderStatus() != OrderStatus.DELIVERED) {
                throw new APIException("Returns only allowed for delivered orders");
            }

            // SpEL Policy Check
            Map<String, Object> context = new HashMap<>();
            context.put("order", order);
            context.put("now", LocalDateTime.now());

            String policy = "order.deliveredDate != null && order.deliveredDate.plusDays(15).isAfter(now)";

            if (!ruleEngine.evaluate(policy, context)) {
                log.warn("Order: Return blocked for {}: Policy violation.", event.email());
                throw new APIException("Return policy violation: Return window expired.");
            }

            List<ValidatedReturnItem> validatedItems = new ArrayList<>();
            double totalRefund = 0.0;

            for (Map.Entry<Long, Integer> entry : event.itemsToReturn().entrySet()) {
                Long itemId = entry.getKey();
                Integer qty = entry.getValue();

                OrderItem item = orderItemRepo.findById(itemId)
                        .orElseThrow(() -> new APIException("OrderItem not found: " + itemId));

                if (qty > (item.getQuantity() - item.getReturnedQuantity())) {
                    throw new APIException("Invalid return quantity for item: " + item.getProductName());
                }

                // Pro-rata calculation
                java.math.BigDecimal discountPerItem = item.getDiscount() != null && item.getQuantity() > 0 
                    ? item.getDiscount().divide(java.math.BigDecimal.valueOf(item.getQuantity()), java.math.RoundingMode.HALF_UP)
                    : java.math.BigDecimal.ZERO;
                double proRataPrice = item.getOrderedPrice().subtract(discountPerItem).doubleValue();
                double itemRefund = proRataPrice * qty;

                validatedItems.add(new ValidatedReturnItem(itemId, qty, proRataPrice));
                totalRefund += itemRefund;

                // Mark item as return requested
                item.setStatus("RETURN_REQUESTED");
                orderItemRepo.save(item);
            }

            log.info("Order: Return validated for order {}. Total refund: {}", event.orderId(), totalRefund);

            // Publish validation event
            eventPublisher.publishEvent(new ReturnValidatedEvent(
                    event.orderId(),
                    event.email(),
                    event.reason(),
                    event.refundType(),
                    totalRefund,
                    validatedItems));

        } catch (Exception e) {
            log.error("Order: Validation failed for return request on order {} - {}", event.orderId(), e.getMessage());
        }
    }

    /**
     * Listen for return approval and update order module state.
     */
    @EventListener
    public void handleReturnApproved(ReturnApprovedEvent event) {
        log.info("Order: Processing return approval for order {}", event.orderId());

        try {
            for (ReturnApprovedEvent.ApprovedReturnItem aItem : event.items()) {
                OrderItem item = orderItemRepo.findById(aItem.orderItemId())
                        .orElseThrow(() -> new APIException("OrderItem not found: " + aItem.orderItemId()));

                item.setReturnedQuantity(item.getReturnedQuantity() + aItem.quantity());
                item.setStatus("RETURNED");
                orderItemRepo.save(item);
            }

            log.info("Order: Items updated for approved return on order {}", event.orderId());

        } catch (Exception e) {
            log.error("Order: Failed to process return approval for order {} - {}", event.orderId(), e.getMessage());
        }
    }
}
