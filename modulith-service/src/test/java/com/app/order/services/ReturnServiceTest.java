package com.app.order.services;

import com.app.order.entities.*;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.support.repositories.ReturnRequestRepo;
import com.app.support.entities.ReturnRequest;
import com.app.support.entities.ReturnItem;
import com.app.support.domain.ReturnService;
import com.app.erp_sync.gateway.ERPNextService;
import com.app.finance.payment.PaymentService;
import com.app.security.services.WalletService;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import com.app.governance.states.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
    private ERPNextService erpNextService;

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
        testItem.setOrderedPrice(100.0);
        testItem.setDiscount(0.0);
        testItem.setProductName("Test Product");
    }

    @Test
    void testRequestPartialReturn_Success() {
        Map<Long, Integer> itemsToReturn = new HashMap<>();
        itemsToReturn.put(10L, 2);

        when(orderRepo.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepo.findById(10L)).thenReturn(Optional.of(testItem));
        when(returnRequestRepo.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArguments()[0]);

        ReturnRequest request = returnService.requestPartialReturn(1L, userEmail, itemsToReturn, "Sizing", ReturnRequest.RefundType.WALLET);

        assertNotNull(request);
        assertEquals(ReturnRequest.ReturnStatus.REQUESTED, request.getStatus());
        assertEquals(200.0, request.getRefundAmount());
        assertEquals(1, request.getItems().size());
        verify(returnRequestRepo, times(1)).save(any());
    }

    @Test
    void testRequestPartialReturn_ExpiredWindow() {
        testOrder.setDeliveredDate(LocalDateTime.now().minusDays(20));
        when(orderRepo.findById(1L)).thenReturn(Optional.of(testOrder));

        Map<Long, Integer> itemsToReturn = new HashMap<>();
        itemsToReturn.put(10L, 1);

        assertThrows(APIException.class, () -> 
            returnService.requestPartialReturn(1L, userEmail, itemsToReturn, "Reason", ReturnRequest.RefundType.WALLET)
        );
    }

    @Test
    void testRequestPartialReturn_InvalidQuantity() {
        Map<Long, Integer> itemsToReturn = new HashMap<>();
        itemsToReturn.put(10L, 10); // More than available

        when(orderRepo.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderItemRepo.findById(10L)).thenReturn(Optional.of(testItem));

        assertThrows(APIException.class, () -> 
            returnService.requestPartialReturn(1L, userEmail, itemsToReturn, "Reason", ReturnRequest.RefundType.WALLET)
        );
    }

    @Test
    void testApproveReturn_WalletRefund() {
        ReturnRequest request = new ReturnRequest();
        request.setReturnRequestId(100L);
        request.setStatus(ReturnRequest.ReturnStatus.REQUESTED);
        request.setRefundType(ReturnRequest.RefundType.WALLET);
        request.setRefundAmount(200.0);
        request.setOrder(testOrder);
        
        ReturnItem rItem = new ReturnItem();
        rItem.setOrderItem(testItem);
        rItem.setQuantity(2);
        request.getItems().add(rItem);

        when(returnRequestRepo.findById(100L)).thenReturn(Optional.of(request));

        returnService.approveReturn(100L);

        assertEquals(ReturnRequest.ReturnStatus.COMPLETED, request.getStatus());
        assertEquals(2, testItem.getReturnedQuantity());
        verify(walletService, times(1)).credit(eq(userEmail), eq(200.0), anyString(), anyString());
        verify(returnRequestRepo, times(1)).save(request);
    }

    @Test
    void testGetUserReturns() {
        returnService.getUserReturns(userEmail);
        verify(returnRequestRepo, times(1)).findByOrderEmail(userEmail);
    }
}
