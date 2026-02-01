package com.app.finance.payment.controllers;

import com.app.finance.payment.PaymentService;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/webhooks")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    @Autowired
    private PaymentService paymentService;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            // Verify signature
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                log.warn("Invalid Razorpay webhook signature");
                return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
            }

            JSONObject json = new JSONObject(payload);
            String event = json.optString("event");

            if ("payment.captured".equals(event)) {
                JSONObject paymentEntity = json.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String pgOrderId = paymentEntity.optString("order_id");
                String pgPaymentId = paymentEntity.optString("id");

                log.info("Payment captured webhook received for order: {}", pgOrderId);
                paymentService.processPaymentCapture(pgOrderId, pgPaymentId);
            }

            return new ResponseEntity<>("OK", HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage());
            return new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
