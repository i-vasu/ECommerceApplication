package com.app.finance.payloads;

public record PaymentInitResponse(
    Long paymentId,
    String pgOrderId,
    String status
) {}
