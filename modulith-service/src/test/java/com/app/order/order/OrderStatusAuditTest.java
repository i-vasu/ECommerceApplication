package com.app.order.order;

import com.app.commerce.states.OrderStatus;
import com.app.order.entities.Order;
import com.app.order.entities.OrderHistory;
import com.app.order.repositories.OrderHistoryRepo;
import com.app.order.repositories.OrderRepo;
import com.app.commerce.states.OrderStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderStatusAuditTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private OrderHistoryRepo historyRepo;

    @Mock
    private OrderStateMachine stateMachine;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("✅ Auditing: Ensure OrderHistory is saved during status transition")
    void testStatusTransitionAuditing() {
        // Arrange
        Long orderId = 123L;
        String email = "customer@example.com";
        Order order = new Order();
        order.setOrderId(orderId);
        order.setEmail(email);
        order.setOrderStatus(OrderStatus.PENDING);

        when(orderRepo.findOrderByEmailAndOrderId(email, orderId)).thenReturn(order);

        // Act
        orderService.updateOrder(email, orderId, "PAID");

        // Assert
        ArgumentCaptor<OrderHistory> historyCaptor = ArgumentCaptor.forClass(OrderHistory.class);
        verify(historyRepo).save(historyCaptor.capture());

        OrderHistory savedHistory = historyCaptor.getValue();
        assertEquals(OrderStatus.PENDING, savedHistory.getOldStatus());
        assertEquals(OrderStatus.PAID, savedHistory.getNewStatus());
        assertEquals("SYSTEM", savedHistory.getUpdatedBy());
    }
}
