package com.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.entites.Order;
import com.app.entites.Payment;
import com.app.repositories.OrderRepo;
import com.app.repositories.PaymentRepo;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;

class PaymentServiceImplTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private PaymentRepo paymentRepo;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    // Mock the inner OrderClient
    private OrderClient orderClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Mocking the final field 'orders' in RazorpayClient using Reflection
        orderClient = mock(OrderClient.class);
        Field ordersField = RazorpayClient.class.getDeclaredField("orders");
        ordersField.setAccessible(true);
        ordersField.set(razorpayClient, orderClient);
    }

    @Test
    void testCreateInternalOrder_Success() throws Exception {
        Long orderId = 1L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setTotalAmount(500.0);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        // Mock Razorpay Order response
        JSONObject jsonOrder = new JSONObject();
        jsonOrder.put("id", "order_test_123");
        jsonOrder.put("status", "created");

        com.razorpay.Order razorpayOrder = new com.razorpay.Order(jsonOrder);
        when(orderClient.create(any(JSONObject.class))).thenReturn(razorpayOrder);

        String result = paymentService.createInternalOrder(orderId);

        assertEquals("order_test_123", result);
        verify(paymentRepo).save(any(Payment.class));
    }
}
