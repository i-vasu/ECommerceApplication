package com.app.order.order;

import com.app.core.multitenancy.ERPNextCredentialProvider;
import com.app.core.utils.HmacUtils;
import com.app.order.services.ERPNextService;
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
@RequestMapping("/api/webhooks/erpnext")
@RequiredArgsConstructor
public class ERPNextWebhookController {

    private final ERPNextService erpNextService;
    private final ERPNextCredentialProvider credentialProvider;
    private final ObjectMapper objectMapper;

    @PostMapping("/order-status")
    public ResponseEntity<String> handleOrderStatusWebhook(
            @RequestHeader(value = "X-ERPNext-Signature", required = false) String signature,
            HttpServletRequest request) {

        try {
            String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            String secret = credentialProvider.getWebhookSecret();

            if (secret != null && !HmacUtils.verifyHmac(payload, signature, secret)) {
                log.error("Invalid HMAC signature for ERPNext webhook");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            JsonNode root = objectMapper.readTree(payload);
            String orderName = root.path("name").asText();
            String status = root.path("status").asText();

            log.info("Received ERPNext webhook for order: {} with status: {}", orderName, status);

            erpNextService.handleWebhookStatusUpdate(orderName, status, root);

            return ResponseEntity.ok("Webhook processed");
        } catch (IOException e) {
            log.error("Error processing ERPNext webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing payload");
        }
    }
}
