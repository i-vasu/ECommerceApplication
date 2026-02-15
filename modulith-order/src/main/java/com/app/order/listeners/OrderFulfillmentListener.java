package com.app.order.listeners;

import com.app.core.events.OrderPaidEvent;
import com.app.core.events.ShipmentRequestedEvent;
import com.app.order.repositories.OrderRepo;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.app.catalog.repositories.ProductRepo;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;
import com.app.catalog.entities.Product;
import com.app.order.entities.OrderItem;

/**
 * Orchestrates integration events for Order Fulfillment.
 * Converts local domain events to cross-module integration events.
 */
@Component
@RequiredArgsConstructor
public class OrderFulfillmentListener {

    private static final Logger log = LogManager.getLogger(OrderFulfillmentListener.class);

        private final OrderRepo orderRepo;
        private final ProductRepo productRepo;
        private final ApplicationEventPublisher eventPublisher;

        @ApplicationModuleListener
        public void onOrderPaid(OrderPaidEvent event) {
                log.info("OrderFulfillmentListener: Processing OrderPaidEvent for Order ID: {}", event.orderId());

                orderRepo.findById(event.orderId()).ifPresent(order -> {

                        // Map to ShipmentRequestedEvent
                        var shippingAddress = new ShipmentRequestedEvent.ShippingAddress(
                                        "Valued Customer",
                                        "9999999999", // Placeholder or fetch if available
                                        order.getShippingStreet(),
                                        order.getShippingCity(),
                                        order.getShippingState(),
                                        order.getShippingCountry(),
                                        order.getShippingPincode());

                        List<Long> productIds = order.getOrderItems().stream().map(OrderItem::getProductId).toList();
                        Map<Long, Product> productMap = productRepo.findAllById(productIds).stream()
                                        .collect(Collectors.toMap(Product::getProductId, p -> p));

                        var items = order.getOrderItems().stream()
                                        .map(item -> {
                                            var p = productMap.get(item.getProductId());
                                            double w = (p != null) ? p.getKgWeight() : 0.5;
                                            double l = (p != null) ? p.getLengthCm() : 10.0;
                                            double wd = (p != null) ? p.getWidthCm() : 10.0;
                                            double h = (p != null) ? p.getHeightCm() : 10.0;

                                            return new ShipmentRequestedEvent.ShipmentItem(
                                                            item.getProductName(),
                                                            item.getItemCode(),
                                                            item.getQuantity(),
                                                            item.getOrderedPrice(),
                                                            w, l, wd, h
                                            );
                                        })
                                        .collect(Collectors.toList());

                        var shipmentEvent = new ShipmentRequestedEvent(
                                        order.getOrderId(),
                                        order.getEmail(),
                                        shippingAddress,
                                        items,
                                        order.getTotalAmount(),
                                        false // Assuming prepaid since it's OrderPaidEvent
                        );

                        eventPublisher.publishEvent(shipmentEvent);
                        log.info("Published ShipmentRequestedEvent for Order ID: {}", order.getOrderId());
                });
        }

        // Processing OrderCancelledEvent for Shipping is now handled by
        // LogisticsEventListener directly listening to OrderCancelledEvent
        // OR we should publish a ShipmentCancellationRequestedEvent?
        // The LogisticsEventListener I wrote listens to `OrderCancelledEvent`.
        // That is fine, `OrderCancelledEvent` is a public domain event.
        // So we don't need logic here for cancellation.

        // However, previously `OrderCleanupListener` was doing it.
        // The plan says: "Kill OrderCleanupListener".
        // So we just remove the logic from here/CleanupListener.
}
