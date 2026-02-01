package com.app.order.services;

import com.app.finance.payment.PaymentService;
import com.app.governance.states.OrderStatus;
import com.app.order.entities.Order;
import com.app.order.entities.OrderItem;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.repositories.OrderRepo;
import com.app.security.services.WalletService;
import com.app.support.domain.ReturnService;
import com.app.support.entities.ReturnItem;
import com.app.support.entities.ReturnRequest;
import com.app.support.repositories.ReturnRequestRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReturnServiceTest {

    @Mock
    private ReturnRequestRepo returnRequestRepo;
    @Mock
    private OrderRepo orderRepo;
    @Mock
    private OrderItemRepo orderItemRepo;
    @Mock
    private PaymentService paymentService;
    @Mock
    private WalletService walletService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReturnService returnService;

    private Order testOrder;
    private OrderItem testItem;
    private String userEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setEmail(userEmail);
        testOrder.setOrderStatus(OrderStatus.DELIVERED);
        testOrder.setDeliveredDate(LocalDateTime.now().minusDays(2));

        testItem = new OrderItem();
        testItem.setOrderItemId(10L);
        testItem.setQuantity(5);
        testItem.setReturnedQuantity(0);
        testItem.setOrderedPrice(BigDecimal.valueOf(100.0));
        testItem.setDiscount(BigDecimal.ZERO);
        testItem.setProductName("Test Product");
    }

    @Test
    void testRequestPartialReturn_Success() {
        Map<Long, Integer> itemsToReturn = new HashMap<>();
        itemsToReturn.put(10L, 2);

        returnService.requestPartialReturn(1L, userEmail, itemsToReturn, "Sizing", ReturnRequest.RefundType.WALLET);

        verify(eventPublisher, times(1)).publishEvent(any(com.app.core.events.ReturnRequestedEvent.class));
    }

    @Test
    void testRequestPartialReturn_PublishesEvent() {
        Map<Long, Integer> itemsToReturn = new HashMap<>();
        itemsToReturn.put(10L, 1);

        returnService.requestPartialReturn(1L, userEmail, itemsToReturn, "Reason", ReturnRequest.RefundType.WALLET);
        
        verify(eventPublisher).publishEvent(any(com.app.core.events.ReturnRequestedEvent.class));
    }

    // Validation tests removed as they moved to OrderReturnListener

    @Test
    void testApproveReturn_Success() {
        ReturnRequest request = new ReturnRequest();
        request.setReturnRequestId(100L);
        request.setStatus(ReturnRequest.ReturnStatus.REQUESTED);
        request.setRefundType(ReturnRequest.RefundType.WALLET);
        request.setRefundAmount(200.0);
        request.setOrderId(1L);
        
        ReturnItem rItem = new ReturnItem();
        rItem.setOrderItemId(10L);
        rItem.setQuantity(2);
        request.setItems(List.of(rItem));

        when(returnRequestRepo.findById(100L)).thenReturn(Optional.of(request));

        returnService.approveReturn(100L);

        assertEquals(ReturnRequest.ReturnStatus.COMPLETED, request.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(com.app.core.events.ReturnApprovedEvent.class));
        verify(returnRequestRepo, times(1)).save(request);
    }

    @Test
    void testGetUserReturns() {
        returnService.getUserReturns(userEmail);
        verify(returnRequestRepo, times(1)).findByUserEmail(userEmail);
    }
}
