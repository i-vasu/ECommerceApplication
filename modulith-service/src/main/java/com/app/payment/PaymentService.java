package com.app.payment;

import java.util.Map;

import com.app.order.payloads.PaymentDTO;

public interface PaymentService {
    String createInternalOrder(Long orderId);

    PaymentDTO verifyPayment(Long orderId, String paymentId, String signature);

    /**
     * Initiate a refund for a payment
     * 
     * @param paymentId Razorpay payment ID
     * @param amount    Amount in paise (null for full refund)
     * @param notes     Reason for refund
     * @return Refund details
     */
    Map<String, Object> initiateRefund(String paymentId, Long amount, String notes);

    /**
     * Get payment details from Razorpay
     */
    Map<String, Object> getPaymentDetails(String paymentId);

    void processPaymentCapture(String pgOrderId, String pgPaymentId);
}
