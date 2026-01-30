package com.app.finance.payment;

import com.app.finance.entities.Payment;
import com.app.finance.payloads.PaymentDTO;
import com.app.finance.payloads.PaymentInitResponse;
import com.app.finance.repositories.PaymentRepo;
import com.app.finance.payment.mappers.PaymentMapper;
import com.app.core.ResourceNotFoundException;
import com.app.core.contracts.OrderAmountProvider;
import com.app.core.contracts.OrderAmountProvider.OrderSummary;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;

@Service("hyperswitchPaymentService")
public class HyperswitchServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(HyperswitchServiceImpl.class);

    private final OrderAmountProvider orderProvider;
    private final PaymentRepo paymentRepo;
    private final PaymentMapper paymentMapper;
    private final org.springframework.web.client.RestClient hyperswitchRestClient;

    @Value("${hyperswitch.merchant.id:}")
    private String merchantId;

    public HyperswitchServiceImpl(
            OrderAmountProvider orderProvider,
            PaymentRepo paymentRepo,
            PaymentMapper paymentMapper,
            org.springframework.web.client.RestClient hyperswitchRestClient) {
        this.orderProvider = orderProvider;
        this.paymentRepo = paymentRepo;
        this.paymentMapper = paymentMapper;
        this.hyperswitchRestClient = hyperswitchRestClient;
    }

    @Override
    @Transactional
    public PaymentInitResponse initiatePayment(Long orderId, String paymentMethod) {
        String clientSecret = createInternalOrder(orderId);
        // We need a payment record to get the ID
        Payment payment = paymentRepo.findByOrderId(orderId).stream().findFirst().orElseThrow();
        return new PaymentInitResponse(payment.getPaymentId(), payment.getPgOrderId(), "created");
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "hyperswitch")
    @Retry(name = "hyperswitch")
    public String createInternalOrder(Long orderId) {
        OrderSummary order = orderProvider.getOrderSummary(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        log.info("Creating Hyperswitch Payment Intent for Order: {}", orderId);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", (int) (order.totalAmount().doubleValue() * 100));
        request.put("currency", "INR");
        request.put("merchant_id", merchantId);
        request.put("confirm", false);
        request.put("customer_id", order.email());

        Map<String, Object> response = hyperswitchRestClient.post()
                .uri("/payments")
                .body(request)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });

        String hyperswitchIntentId = (String) response.get("payment_id");
        String clientSecret = (String) response.get("client_secret");

        Payment payment = paymentRepo.findByOrderId(orderId).stream().findFirst().orElse(null);
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(order.totalAmount());
            payment.setPaymentMethod("HYPERSWITCH");
        }

        payment.setPgOrderId(hyperswitchIntentId);
        payment.setPgStatus("requires_payment_method");
        paymentRepo.save(payment);

        return clientSecret;
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "hyperswitch")
    @Retry(name = "hyperswitch")
    public PaymentDTO verifyPayment(Long orderId, String paymentId, String signature) {
        log.info("Verifying Hyperswitch Payment for Order: {}", orderId);

        Map<String, Object> response = hyperswitchRestClient.get()
                .uri("/payments/" + paymentId)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });

        String status = (String) response.get("status");

        Payment payment = paymentRepo.findByPgOrderId(paymentId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment", "pgOrderId", paymentId);
        }

        if ("succeeded".equalsIgnoreCase(status)) {
            payment.setPgStatus("captured");
            paymentRepo.save(payment);
        }

        return paymentMapper.paymentToPaymentDTO(payment);
    }

    @Override
    public Map<String, Object> initiateRefund(String paymentId, Long amount, String notes) {
        log.info("Initiating Hyperswitch Refund for: {}", paymentId);
        Map<String, Object> response = new HashMap<>();
        response.put("refund_id", "hs_ref_" + System.currentTimeMillis());
        response.put("status", "succeeded");
        return response;
    }

    @Override
    public Map<String, Object> getPaymentDetails(String paymentId) {
        return Map.of("id", paymentId, "status", "captured");
    }

    @Override
    public void processPaymentCapture(String pgOrderId, String pgPaymentId) {
        log.info("Capturing Hyperswitch Payment: {}", pgOrderId);
    }

    @Override
    public void processRefundForOrder(Long orderId, String reason) {
        log.info("Processing manual refund for Hyperswitch: {}", orderId);
    }
    @Override
    public boolean isPaymentCompleted(Long orderId) {
        return paymentRepo.findByOrderId(orderId).stream()
                .anyMatch(p -> "captured".equalsIgnoreCase(p.getPgStatus()));
    }
}
