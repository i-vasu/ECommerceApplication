package com.app.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import com.app.entites.Order;
import com.app.entites.Payment;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.PaymentDTO;
import com.app.repositories.OrderRepo;
import com.app.repositories.PaymentRepo;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

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

    private static final String TEST_SECRET = "test_razorpay_secret";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Mocking the field 'orders' in RazorpayClient using Reflection
        orderClient = mock(OrderClient.class);
        Field ordersField = RazorpayClient.class.getDeclaredField("orders");
        ordersField.setAccessible(true);
        ordersField.set(razorpayClient, orderClient);

        // Set secret for signature verification
        ReflectionTestUtils.setField(paymentService, "secret", TEST_SECRET);
    }

    @Test
    @DisplayName("Create Internal Order - Success")
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
        assertNotNull(order.getPayment());
        assertEquals("created", order.getPayment().getPgStatus());
        assertEquals("RAZORPAY", order.getPayment().getPaymentMethod());
    }

    @Test
    @DisplayName("Create Internal Order - Order Not Found")
    void testCreateInternalOrder_OrderNotFound() {
        when(orderRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createInternalOrder(99L));
        verify(paymentRepo, never()).save(any());
    }

    @Test
    @DisplayName("Create Internal Order - Razorpay Exception")
    void testCreateInternalOrder_RazorpayException() throws Exception {
        Long orderId = 1L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setTotalAmount(1000.0);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));
        when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("API Error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.createInternalOrder(orderId));
        assertEquals("Error creating Razorpay order: API Error", exception.getMessage());
    }

    @Test
    @DisplayName("Create Internal Order - Existing Payment Updated")
    void testCreateInternalOrder_ExistingPaymentUpdated() throws Exception {
        Long orderId = 1L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setTotalAmount(750.0);

        Payment existingPayment = new Payment();
        existingPayment.setPaymentId(10L);
        existingPayment.setPgStatus("pending");
        order.setPayment(existingPayment);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        JSONObject jsonOrder = new JSONObject();
        jsonOrder.put("id", "order_updated_456");
        jsonOrder.put("status", "created");

        com.razorpay.Order razorpayOrder = new com.razorpay.Order(jsonOrder);
        when(orderClient.create(any(JSONObject.class))).thenReturn(razorpayOrder);

        String result = paymentService.createInternalOrder(orderId);

        assertEquals("order_updated_456", result);
        assertEquals("order_updated_456", existingPayment.getPgOrderId());
        assertEquals("created", existingPayment.getPgStatus());
    }

    @Test
    @DisplayName("Verify Payment - Success with Valid Signature")
    void testVerifyPayment_Success() throws Exception {
        Long orderId = 1L;
        String paymentId = "pay_abc123";
        String pgOrderId = "order_xyz789";

        Order order = new Order();
        order.setOrderId(orderId);

        Payment payment = new Payment();
        payment.setPgOrderId(pgOrderId);
        order.setPayment(payment);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        // Generate valid HMAC signature
        String payload = pgOrderId + "|" + paymentId;
        String validSignature = generateHmacSignature(payload, TEST_SECRET);

        PaymentDTO expectedDTO = new PaymentDTO();
        expectedDTO.setPgPaymentId(paymentId);
        when(modelMapper.map(any(Payment.class), any())).thenReturn(expectedDTO);

        PaymentDTO result = paymentService.verifyPayment(orderId, paymentId, validSignature);

        assertNotNull(result);
        assertEquals("captured", payment.getPgStatus());
        assertEquals(paymentId, payment.getPgPaymentId());
        assertEquals(validSignature, payment.getPgSignature());
        verify(paymentRepo).save(payment);
    }

    @Test
    @DisplayName("Verify Payment - Order Not Found")
    void testVerifyPayment_OrderNotFound() {
        when(orderRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.verifyPayment(99L, "pay_123", "sig"));
    }

    @Test
    @DisplayName("Verify Payment - No Payment Initialized")
    void testVerifyPayment_NoPaymentInitialized() {
        Long orderId = 1L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setPayment(null); // No payment

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.verifyPayment(orderId, "pay_123", "sig"));
        assertEquals("Payment initialization missing for order " + orderId, exception.getMessage());
    }

    @Test
    @DisplayName("Verify Payment - Invalid Signature")
    void testVerifyPayment_InvalidSignature() {
        Long orderId = 1L;
        String paymentId = "pay_abc123";
        String pgOrderId = "order_xyz789";

        Order order = new Order();
        order.setOrderId(orderId);

        Payment payment = new Payment();
        payment.setPgOrderId(pgOrderId);
        order.setPayment(payment);

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        String invalidSignature = "invalid_signature_here";

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentService.verifyPayment(orderId, paymentId, invalidSignature));
        assertEquals("Payment signature verification failed", exception.getMessage());
        verify(paymentRepo, never()).save(any());
    }

    @Test
    @DisplayName("Create Internal Order - Amount Conversion to Paisa")
    void testCreateInternalOrder_PaisaConversion() throws Exception {
        Long orderId = 1L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setTotalAmount(99.99); // Should become 9999 paisa

        when(orderRepo.findById(orderId)).thenReturn(Optional.of(order));

        JSONObject jsonOrder = new JSONObject();
        jsonOrder.put("id", "order_paisa_test");
        jsonOrder.put("status", "created");

        com.razorpay.Order razorpayOrder = new com.razorpay.Order(jsonOrder);
        when(orderClient.create(any(JSONObject.class))).thenAnswer(invocation -> {
            JSONObject request = invocation.getArgument(0);
            // Verify amount is in paisa (99.99 * 100 = 9999)
            assertEquals(9999, request.getInt("amount"));
            assertEquals("INR", request.getString("currency"));
            return razorpayOrder;
        });

        paymentService.createInternalOrder(orderId);
    }

    // Helper method to generate HMAC signature
    private String generateHmacSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes());
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
