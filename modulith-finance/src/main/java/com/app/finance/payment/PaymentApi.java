package com.app.finance.payment;

import com.app.catalog.payloads.APIResponse;
import com.app.core.payloads.PaymentDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "Payments", description = "Razorpay Payment Gateway Integration and Management")
public interface PaymentApi {

    @Operation(summary = "Create Razorpay Order", description = "Initializes a transaction with Razorpay and returns a unique gateway Order ID.")
    @ApiResponse(responseCode = "200", description = "Razorpay order created successfully")
    ResponseEntity<APIResponse> createOrder(
            @Parameter(description = "Internal Order ID to be paid") Long orderId);

    @Operation(summary = "Verify Payment Signature", description = "Validates the Razorpay signature to prevent tampering. This must be called after the user completes the UI payment flow.")
    @ApiResponse(responseCode = "200", description = "Payment verified and captured")
    @ApiResponse(responseCode = "400", description = "Invalid signature or payment failed")
    ResponseEntity<PaymentDTO> verifyPayment(
            @Parameter(description = "Internal Order ID") Long orderId,
            @Parameter(description = "Razorpay Payment ID") String paymentId,
            @Parameter(description = "Razorpay Signature") String signature);

    @Operation(summary = "Fetch Gateway Details", description = "Retrieves raw payment details directly from Razorpay for auditing.")
    @ApiResponse(responseCode = "200", description = "Details retrieved")
    ResponseEntity<Map<String, Object>> getPaymentDetails(
            @Parameter(description = "Razorpay Payment ID") String paymentId);

    @Operation(summary = "Request Refund", description = "Initiates a refund for a successful payment. Can be partial if amount is provided.")
    @ApiResponse(responseCode = "200", description = "Refund initiated")
    ResponseEntity<Map<String, Object>> initiateRefund(
            @Parameter(description = "Razorpay Payment ID") String paymentId,
            @Parameter(description = "Refund details (amount in paise, notes)") PaymentController.RefundRequest refundRequest);
}
