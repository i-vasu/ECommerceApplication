package com.app.finance.payment;

import com.app.finance.payloads.PaymentDTO;
import com.app.finance.payloads.PaymentInitResponse;

import java.util.Map;

public interface PaymentService {

    // Legacy/Internal order creation for backward compatibility with existing
    // controller
    String createInternalOrder(Long orderId);

    PaymentInitResponse initiatePayment(Long orderId, String paymentMethod);

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

    // Decoupling addition: direct call from listener
    void processRefundForOrder(Long orderId, String reason);

    /**
     * Get payment details from Razorpay
     */
    Map<String, Object> getPaymentDetails(String paymentId);

    void processPaymentCapture(String pgOrderId, String pgPaymentId);

    /**
     * Check if payment is completed for an order
     */
    boolean isPaymentCompleted(Long orderId);
}
