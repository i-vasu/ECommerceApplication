package com.app.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.app.finance.payment.mappers.PaymentMapper;
import com.app.finance.repositories.PaymentRepo;
import com.app.finance.payment.PaymentServiceImpl;
import com.app.order.repositories.OrderRepo;
import com.app.order.entities.Order;
import com.app.finance.entities.Payment;
import com.app.finance.payloads.PaymentDTO;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import java.util.Optional;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepo paymentRepo;
    @Mock
    private OrderRepo orderRepo;

    // Deep stubs for Razorpay client (orders.create, payments.refund etc)
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RazorpayClient razorpayClient;

    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createInternalOrder_Success() throws RazorpayException {
        // Arrange
        Long orderId = 101L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setTotalAmount(500.0);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        // Mock Razorpay Order
        com.razorpay.Order mockRzOrder = mock(com.razorpay.Order.class);
        when(mockRzOrder.get("id")).thenReturn("order_rzp_123");

        // Fix for NullPointerException: Mockito deep stubs don't handle public fields
        // well.
        // We must manually set the 'orders' field on the mocked RazorpayClient.
        com.razorpay.OrderClient mockOrderClient = mock(com.razorpay.OrderClient.class);
        ReflectionTestUtils.setField(razorpayClient, "orders", mockOrderClient);

        when(mockOrderClient.create(any(JSONObject.class))).thenReturn(mockRzOrder);

        // Act
        String pgOrderId = paymentService.createInternalOrder(orderId);

        // Assert
        assertEquals("order_rzp_123", pgOrderId);
    }

    @Test
    void verifyPayment_SignatureFailure() {
        // Arrange
        Long orderId = 101L;
        String payId = "pay_123";
        String sign = "invalid_sign";

        Order order = new Order();
        Payment payment = new Payment();
        payment.setPgOrderId("order_rzp_123");
        order.setPayment(payment);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        // Inject secret using Reflection (as it is @Value)
        ReflectionTestUtils.setField(paymentService, "secret", "test_secret");

        // Strategy: Mock standard failure behavior.
        // Since verifyPaymentSignature computes HMAC, dummy data will return false.

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.verifyPayment(orderId, payId, sign));

        assertEquals("Payment signature verification failed", exception.getMessage());
    }

    @Test
    void verifyPayment_Success() throws RazorpayException {
        // Arrange
        Long orderId = 101L;
        String payId = "pay_123";
        String sign = "valid_sign";

        Order order = new Order();
        Payment payment = new Payment();
        payment.setPgOrderId("order_rzp_123");
        order.setPayment(payment);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(paymentService, "secret", "test_secret");

        PaymentDTO expectedDto = new PaymentDTO(null, null, null, null, null, "captured");
        when(paymentMapper.paymentToPaymentDTO(any(Payment.class))).thenReturn(expectedDto);

        // Act & Assert
        try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                    .thenReturn(true);

            PaymentDTO result = paymentService.verifyPayment(orderId, payId, sign);

            assertEquals(expectedDto, result);
            assertEquals("captured", payment.getPgStatus());
            assertEquals(payId, payment.getPgPaymentId());
            assertEquals(sign, payment.getPgSignature());

            verify(paymentRepo).save(payment);
        }
    }
}
