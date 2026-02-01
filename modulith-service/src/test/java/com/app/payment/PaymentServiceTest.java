package com.app.payment;

import com.app.finance.entities.Payment;
import com.app.finance.payloads.PaymentDTO;
import com.app.finance.payment.PaymentServiceImpl;
import com.app.finance.payment.mappers.PaymentMapper;
import com.app.finance.repositories.PaymentRepo;
import com.app.order.entities.Order;
import com.app.order.repositories.OrderRepo;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
    private tools.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private com.app.core.contracts.OrderAmountProvider orderProvider;

    @Mock
    private com.app.core.multitenancy.RazorpayCredentialProvider credentialProvider;
    @Mock
    private com.app.security.services.WalletService walletService;
    @Mock
    private com.app.core.services.RedisLockService lockService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createInternalOrder_Success() throws RazorpayException {
        // Arrange
        Long orderId = 101L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setTotalAmount(java.math.BigDecimal.valueOf(500.0));

        lenient().when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        lenient().when(orderProvider.getOrderSummary(orderId)).thenReturn(Optional.of(new com.app.core.contracts.OrderAmountProvider.OrderSummary(orderId, "test@test.com", java.math.BigDecimal.valueOf(500.0), "PENDING")));
        lenient().when(walletService.getBalance(anyString())).thenReturn(1000.0);
        lenient().when(credentialProvider.getKeyId()).thenReturn("test_key");
        lenient().when(credentialProvider.getKeySecret()).thenReturn("test_secret");

        // Mock Razorpay Order
        com.razorpay.Order mockRzOrder = mock(com.razorpay.Order.class);
        lenient().when(mockRzOrder.get("id")).thenReturn("order_rzp_123");

        lenient().when(paymentRepo.findByOrderId(orderId)).thenReturn(java.util.List.of());
        lenient().when(paymentRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String pgOrderId;
        try (org.mockito.MockedConstruction<RazorpayClient> mocked = org.mockito.Mockito.mockConstruction(RazorpayClient.class,
                (mock, context) -> {
                    com.razorpay.OrderClient mockOrderClient = mock(com.razorpay.OrderClient.class);
                    ReflectionTestUtils.setField(mock, "orders", mockOrderClient);
                    when(mockOrderClient.create(any(JSONObject.class))).thenReturn(mockRzOrder);
                })) {
            pgOrderId = paymentService.createInternalOrder(orderId);
        }

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
        
        lenient().when(paymentRepo.findByOrderId(orderId)).thenReturn(java.util.List.of(payment));
        lenient().when(lockService.tryLock(anyString(), any())).thenReturn(true);

        // Mock credential provider secret
        lenient().when(credentialProvider.getKeySecret()).thenReturn("test_secret");

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

        Payment payment = new Payment();
        payment.setPgOrderId("order_rzp_123");
        
        lenient().when(paymentRepo.findByOrderId(orderId)).thenReturn(java.util.List.of(payment));
        lenient().when(paymentRepo.findByPgOrderId("order_rzp_123")).thenReturn(payment);
        lenient().when(orderProvider.getOrderSummary(orderId)).thenReturn(Optional.of(new com.app.core.contracts.OrderAmountProvider.OrderSummary(orderId, "test@test.com", java.math.BigDecimal.valueOf(500.0), "PENDING")));
        lenient().when(credentialProvider.getKeySecret()).thenReturn("test_secret");
        lenient().when(lockService.tryLock(anyString(), any())).thenReturn(true);

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
