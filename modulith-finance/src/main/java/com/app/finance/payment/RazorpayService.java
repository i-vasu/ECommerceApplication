package com.app.finance.payment;

import com.razorpay.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Production-ready Razorpay Payment Integration Service
 * 
 * Features:
 * - Create payment orders
 * - Verify payment signatures (HMAC SHA256)
 * - Capture payments
 * - Process refunds (full and partial)
 * - Fetch payment details
 * 
 * API Documentation: https://razorpay.com/docs/api/
 */
@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    private RazorpayClient razorpayClient;

    /**
     * Get or create Razorpay client instance
     */
    private RazorpayClient getClient() throws RazorpayException {
        if (razorpayClient == null) {
            razorpayClient = new RazorpayClient(keyId, keySecret);
        }
        return razorpayClient;
    }

    /**
     * Create a Razorpay order
     * 
     * @param amount  Amount in smallest currency unit (paise for INR)
     * @param receipt Unique receipt/order ID from your system
     * @param notes   Additional notes/metadata
     * @return Map containing order_id, amount, currency, etc.
     */
    public Map<String, Object> createOrder(long amount, String receipt, Map<String, String> notes) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount); // amount in paise
            orderRequest.put("currency", defaultCurrency);
            orderRequest.put("receipt", receipt);

            if (notes != null && !notes.isEmpty()) {
                JSONObject notesJson = new JSONObject();
                notes.forEach(notesJson::put);
                orderRequest.put("notes", notesJson);
            }

            log.info("Creating Razorpay order: amount={}, receipt={}", amount, receipt);

            Order order = getClient().orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("order_id", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("amount_due", order.get("amount_due"));
            response.put("currency", order.get("currency"));
            response.put("receipt", order.get("receipt"));
            response.put("status", order.get("status"));
            response.put("key_id", keyId); // For frontend to initiate checkout

            log.info("Razorpay order created: {}", String.valueOf(order.get("id")));
            return response;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Payment order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create Razorpay order from order amount in rupees
     * 
     * @param amountInRupees Amount in rupees (will be converted to paise)
     * @param orderId        Your system's order ID
     * @return Order details
     */
    public Map<String, Object> createOrderFromAmount(double amountInRupees, Long orderId) {
        long amountInPaise = Math.round(amountInRupees * 100);
        Map<String, String> notes = new HashMap<>();
        notes.put("order_id", String.valueOf(orderId));
        return createOrder(amountInPaise, "order_" + orderId, notes);
    }

    /**
     * Verify payment signature to ensure payment authenticity
     * This is CRITICAL for security - never skip this step!
     * 
     * @param razorpayOrderId   The order_id from createOrder
     * @param razorpayPaymentId The payment_id from Razorpay callback
     * @param razorpaySignature The signature from Razorpay callback
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String expectedSignature = generateHmacSha256(payload, keySecret);

            boolean isValid = expectedSignature.equals(razorpaySignature);

            if (isValid) {
                log.info("Payment signature verified: payment_id={}", razorpayPaymentId);
            } else {
                log.warn("Payment signature verification FAILED: payment_id={}", razorpayPaymentId);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying payment signature", e);
            return false;
        }
    }

    /**
     * Fetch payment details by payment ID
     */
    public Map<String, Object> getPaymentDetails(String paymentId) {
        try {
            Payment payment = getClient().payments.fetch(paymentId);

            Map<String, Object> details = new HashMap<>();
            details.put("id", payment.get("id"));
            details.put("amount", payment.get("amount"));
            details.put("currency", payment.get("currency"));
            details.put("status", payment.get("status"));
            details.put("method", payment.get("method"));
            details.put("email", payment.get("email"));
            details.put("contact", payment.get("contact"));
            details.put("order_id", payment.get("order_id"));
            details.put("captured", payment.get("captured"));
            details.put("created_at", payment.get("created_at"));

            return details;
        } catch (RazorpayException e) {
            log.error("Failed to fetch payment details: {}", paymentId, e);
            throw new RuntimeException("Failed to fetch payment: " + e.getMessage(), e);
        }
    }

    /**
     * Capture an authorized payment
     * Use this if you've disabled auto-capture in Razorpay dashboard
     * 
     * @param paymentId The payment ID to capture
     * @param amount    Amount to capture in paise (must be <= authorized amount)
     * @return Captured payment details
     */
    public Map<String, Object> capturePayment(String paymentId, long amount) {
        try {
            JSONObject captureRequest = new JSONObject();
            captureRequest.put("amount", amount);
            captureRequest.put("currency", defaultCurrency);

            log.info("Capturing payment: id={}, amount={}", paymentId, amount);

            Payment payment = getClient().payments.capture(paymentId, captureRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("id", payment.get("id"));
            response.put("amount", payment.get("amount"));
            response.put("status", payment.get("status"));
            response.put("captured", payment.get("captured"));

            log.info("Payment captured successfully: {}", paymentId);
            return response;
        } catch (RazorpayException e) {
            log.error("Failed to capture payment: {}", paymentId, e);
            throw new RuntimeException("Payment capture failed: " + e.getMessage(), e);
        }
    }

    /**
     * Initiate a full refund for a payment
     */
    public Map<String, Object> refundFull(String paymentId, String notes) {
        return refund(paymentId, null, notes);
    }

    /**
     * Initiate a partial refund for a payment
     * 
     * @param paymentId The payment ID to refund
     * @param amount    Amount to refund in paise
     * @param notes     Reason for refund
     * @return Refund details
     */
    public Map<String, Object> refund(String paymentId, Long amount, String notes) {
        try {
            JSONObject refundRequest = new JSONObject();
            if (amount != null) {
                refundRequest.put("amount", amount);
            }
            if (notes != null && !notes.isEmpty()) {
                JSONObject notesJson = new JSONObject();
                notesJson.put("reason", notes);
                refundRequest.put("notes", notesJson);
            }
            refundRequest.put("speed", "normal"); // "normal" or "optimum"

            log.info("Initiating refund: payment_id={}, amount={}", paymentId, amount);

            Refund refund = getClient().payments.refund(paymentId, refundRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("refund_id", refund.get("id"));
            response.put("payment_id", refund.get("payment_id"));
            response.put("amount", refund.get("amount"));
            response.put("status", refund.get("status"));
            response.put("speed_requested", refund.get("speed_requested"));

            log.info("Refund initiated: refund_id={}", String.valueOf(refund.get("id")));
            return response;
        } catch (RazorpayException e) {
            log.error("Failed to initiate refund for payment: {}", paymentId, e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get order details from Razorpay
     */
    public Map<String, Object> getOrderDetails(String orderId) {
        try {
            Order order = getClient().orders.fetch(orderId);

            Map<String, Object> details = new HashMap<>();
            details.put("id", order.get("id"));
            details.put("amount", order.get("amount"));
            details.put("amount_paid", order.get("amount_paid"));
            details.put("amount_due", order.get("amount_due"));
            details.put("currency", order.get("currency"));
            details.put("receipt", order.get("receipt"));
            details.put("status", order.get("status"));
            details.put("attempts", order.get("attempts"));

            return details;
        } catch (RazorpayException e) {
            log.error("Failed to fetch order details: {}", orderId, e);
            throw new RuntimeException("Failed to fetch order: " + e.getMessage(), e);
        }
    }

    /**
     * Generate HMAC SHA256 signature
     */
    private String generateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes("UTF-8"));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Get the public key ID (for frontend)
     */
    public String getKeyId() {
        return keyId;
    }
}
