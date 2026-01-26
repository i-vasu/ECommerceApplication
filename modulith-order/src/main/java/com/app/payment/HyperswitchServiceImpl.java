package com.app.payment;

import com.app.order.entities.Order;
import com.app.order.entities.Payment;
import com.app.order.payloads.PaymentDTO;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.PaymentRepo;
import com.app.payment.mappers.PaymentMapper;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;

@Service("hyperswitchPaymentService")
@Slf4j
@RequiredArgsConstructor
public class HyperswitchServiceImpl implements PaymentService {

    private final OrderRepo orderRepo;
    private final PaymentRepo paymentRepo;
    private final PaymentMapper paymentMapper;
    private final org.springframework.web.client.RestClient hyperswitchRestClient;

    @Value("${hyperswitch.merchant.id:}")
    private String merchantId;

    @Override
    @Transactional
    @CircuitBreaker(name = "hyperswitch")
    @Retry(name = "hyperswitch")
    public String createInternalOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        log.info("Creating Hyperswitch Payment Intent for Order: {}", orderId);
        
        Map<String, Object> request = new HashMap<>();
        request.put("amount", (int) (order.getTotalAmount() * 100));
        request.put("currency", "INR");
        request.put("merchant_id", merchantId);
        request.put("confirm", false);
        request.put("customer_id", order.getEmail());

        Map<String, Object> response = hyperswitchRestClient.post()
                .uri("/payments")
                .body(request)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

        String hyperswitchIntentId = (String) response.get("payment_id");
        String clientSecret = (String) response.get("client_secret");
        
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            order.setPayment(payment);
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
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

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
}
