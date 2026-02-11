package com.app.finance.listeners;

import com.app.core.events.EventIdempotencyService;
import com.app.core.events.OrderCancelledEvent;
import com.app.finance.payment.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceEventListenerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private EventIdempotencyService idempotencyService;

    @InjectMocks
    private FinanceEventListener financeEventListener;

    @Test
    @DisplayName("BEHAVIOR: Should initiate refund when order is cancelled")
    void onOrderCancelled_SuccessfulRefund() {
        // Arrange
        Long orderId = 1L;
        String reason = "User Request";
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, 101L, 5000.0, reason, java.util.Collections.emptyList());
        String eventId = "ORDER-CANCEL-FINANCE-" + orderId;

        when(idempotencyService.isEventProcessed(eventId, "FINANCE_MODULE", "OrderCancelledEvent")).thenReturn(false);

        // Act
        financeEventListener.onOrderCancelled(event);

        // Assert
        verify(paymentService).processRefundForOrder(orderId, reason);
        verify(idempotencyService).markEventAsProcessed(eventId, "FINANCE_MODULE", "OrderCancelledEvent");
    }

    @Test
    @DisplayName("BEHAVIOR: Should skip processing if event is already processed (Idempotency)")
    void onOrderCancelled_AlreadyProcessed_ShouldSkip() {
        // Arrange
        Long orderId = 2L;
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, 101L, 5000.0, "Duplicate", java.util.Collections.emptyList());
        String eventId = "ORDER-CANCEL-FINANCE-" + orderId;

        when(idempotencyService.isEventProcessed(eventId, "FINANCE_MODULE", "OrderCancelledEvent")).thenReturn(true);

        // Act
        financeEventListener.onOrderCancelled(event);

        // Assert
        verify(paymentService, never()).processRefundForOrder(anyLong(), anyString());
        verify(idempotencyService, never()).markEventAsProcessed(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("ERROR: Should handle exceptions and NOT mark as processed on failure")
    void onOrderCancelled_PaymentServiceError_ShouldNotMarkProcessed() {
        // Arrange
        Long orderId = 3L;
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, 101L, 5000.0, "System Error", java.util.Collections.emptyList());
        String eventId = "ORDER-CANCEL-FINANCE-" + orderId;

        when(idempotencyService.isEventProcessed(eventId, "FINANCE_MODULE", "OrderCancelledEvent")).thenReturn(false);
        doThrow(new RuntimeException("Payment Gateway Down")).when(paymentService).processRefundForOrder(anyLong(), anyString());

        // Act
        financeEventListener.onOrderCancelled(event);

        // Assert
        verify(idempotencyService, never()).markEventAsProcessed(anyString(), anyString(), anyString());
    }
}
