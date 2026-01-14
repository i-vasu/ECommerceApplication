package com.app.services;

import com.app.payloads.PaymentDTO;

public interface PaymentService {
    String createInternalOrder(Long orderId);

    PaymentDTO verifyPayment(Long orderId, String paymentId, String signature);
}
