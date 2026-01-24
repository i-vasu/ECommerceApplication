package com.app.payment;

import com.app.product.payloads.APIResponse;
import com.app.order.payloads.PaymentDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Tag(name = "Payments", description = "Razorpay Payment Gateway Integration")
public interface PaymentApi {

    @Operation(summary = "Create Payment Order", description = "Creates a Razorpay order for the specified internal order")
    ResponseEntity<APIResponse> createOrder(Long orderId);

    @Operation(summary = "Verify Payment", description = "Verifies the payment signature after Razorpay checkout")
    ResponseEntity<PaymentDTO> verifyPayment(Long orderId, String paymentId, String signature);

    @Operation(summary = "Get Payment Details", description = "Fetches payment details from Razorpay")
    ResponseEntity<Map<String, Object>> getPaymentDetails(String paymentId);

    @Operation(summary = "Initiate Refund", description = "Initiates a full or partial refund for a payment")
    ResponseEntity<Map<String, Object>> initiateRefund(String paymentId, PaymentController.RefundRequest refundRequest);
}
