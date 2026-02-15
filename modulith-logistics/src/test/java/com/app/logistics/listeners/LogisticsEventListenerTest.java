package com.app.logistics.listeners;

import com.app.core.events.OrderCancelledEvent;
import com.app.logistics.inventory.InventoryService;
import com.app.logistics.shipping.ShipmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LogisticsEventListenerTest {

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private LogisticsEventListener logisticsEventListener;

    @Test
    void testOnOrderCancelled_ShouldCancelShipment() {
        // Arrange
        Long orderId = 1L;
        // Using correct constructor or mock event if possible, but existing code used concrete event
        // The event constructor signature in test: (Long, Long, Double, String, List)
        // Which matches OrderCancelledEvent(orderId, customerId, amount, reason, items)
        OrderCancelledEvent.CancelledItem item = new OrderCancelledEvent.CancelledItem("ITEM001", 2);
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, 1L, 100.0, "Customer Cancelled", List.of(item));

        // Act
        logisticsEventListener.onOrderCancelled(event);

        // Assert
        verify(shipmentService).cancelShipmentByOrderId(orderId);
    }
}
