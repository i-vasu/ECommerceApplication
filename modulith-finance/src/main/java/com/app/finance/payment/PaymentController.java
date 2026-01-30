package com.app.finance.payment;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.catalog.payloads.APIResponse;
import com.app.finance.payloads.PaymentDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Payment Controller for Razorpay Integration
 * 
 * Production-ready endpoints:
 * - POST /api/payment/create/{orderId} - Create Razorpay order
 * - POST /api/payment/verify - Verify payment signature
 * - GET /api/payment/{paymentId} - Get payment details
 * - POST /api/payment/refund/{paymentId} - Initiate refund
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments", description = "Razorpay payment management APIs")
@SecurityRequirement(name = "E-Commerce Application")
public class PaymentController implements PaymentApi {

    @Autowired
    private PaymentService paymentService;

    /**
     * Create a Razorpay order for payment
     */
    @PostMapping("/create/{orderId}")
    @Override
    public ResponseEntity<APIResponse> createOrder(@PathVariable Long orderId) {
        try {
            String razorpayOrderId = paymentService.createInternalOrder(orderId);
            return new ResponseEntity<>(new APIResponse(razorpayOrderId, true), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new APIResponse("Failed to create payment: " + e.getMessage(), false),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify payment after Razorpay checkout completion
     * This MUST be called to verify payment authenticity
     */
    @PostMapping("/verify")
    @Override
    public ResponseEntity<PaymentDTO> verifyPayment(
            @RequestParam Long orderId,
            @RequestParam String paymentId,
            @RequestParam String signature) {
        PaymentDTO paymentDTO = paymentService.verifyPayment(orderId, paymentId, signature);
        return new ResponseEntity<>(paymentDTO, HttpStatus.OK);
    }

    /**
     * Get payment details from Razorpay
     */
    @GetMapping("/{paymentId}")
    @Override
    public ResponseEntity<Map<String, Object>> getPaymentDetails(@PathVariable String paymentId) {
        try {
            Map<String, Object> details = paymentService.getPaymentDetails(paymentId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Initiate refund for a payment
     */
    @PostMapping("/refund/{paymentId}")
    @Override
    public ResponseEntity<Map<String, Object>> initiateRefund(
            @PathVariable String paymentId,
            @RequestBody(required = false) RefundRequest refundRequest) {
        try {
            Long amount = refundRequest != null ? refundRequest.getAmount() : null;
            String notes = refundRequest != null ? refundRequest.getNotes() : "Customer requested refund";

            Map<String, Object> refund = paymentService.initiateRefund(paymentId, amount, notes);
            return ResponseEntity.ok(refund);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Request DTO ====================

    public static class RefundRequest {
        private Long amount; // Amount in paise (null for full refund)
        private String notes;

        public Long getAmount() {
            return amount;
        }

        public void setAmount(Long amount) {
            this.amount = amount;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
