package com.app.payment;

import com.app.core.multitenancy.RazorpayCredentialProvider;
import com.app.core.utils.HmacUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final PaymentService paymentService;
    private final RazorpayCredentialProvider credentialProvider;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            HttpServletRequest request) {

        try {
            String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            String secret = credentialProvider.getWebhookSecret();

            if (!HmacUtils.verifyHmac(payload, signature, secret)) {
                log.error("Invalid Razorpay webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();

            log.info("Received Razorpay webhook: {}", event);

            if ("payment.captured".equals(event)) {
                JsonNode paymentNode = root.path("payload").path("payment").path("entity");
                String pgOrderId = paymentNode.path("order_id").asText();
                String pgPaymentId = paymentNode.path("id").asText();

                paymentService.processPaymentCapture(pgOrderId, pgPaymentId);
            }

            return ResponseEntity.ok("Webhook processed");
        } catch (IOException e) {
            log.error("Error reading Razorpay webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error reading payload");
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing failed");
        }
    }
}
