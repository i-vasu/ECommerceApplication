package com.app.finance.payment;

import com.app.catalog.review.services.SocialProofService;
import com.app.core.ResourceNotFoundException;
import com.app.core.async.EventProducer;
import com.app.core.audit.AuditTrail;
import com.app.core.contracts.OrderAmountProvider;
import com.app.core.contracts.OrderAmountProvider.OrderSummary;
import com.app.core.events.OrderPaidEvent;
import com.app.core.events.OrderStatusEvent;
import com.app.core.multitenancy.RazorpayCredentialProvider;
import com.app.core.payloads.PaymentDTO;
import com.app.core.services.RedisLockService;
import com.app.finance.entities.Payment;
import com.app.finance.payment.mappers.PaymentMapper;
import com.app.finance.repositories.PaymentRepo;
import com.app.finance.repositories.RefundRepo;
import com.app.security.services.WalletService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@org.springframework.context.annotation.Primary
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    private RazorpayCredentialProvider credentialProvider;

    private RazorpayClient getRazorpayClient() {
        try {
            return new RazorpayClient(credentialProvider.getKeyId(), credentialProvider.getKeySecret());
        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to initialize Razorpay Client", e);
        }
    }

    @Autowired
    private OrderAmountProvider orderProvider;

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WalletService walletService;

    @Autowired
    private SocialProofService socialProofService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private RefundRepo refundRepo;

    @Autowired
    private RedisLockService lockService;

    @Autowired
    private EventProducer eventProducer;

    @Override
    @Transactional
    public void processPaymentCapture(String pgOrderId, String pgPaymentId) {
        Payment payment = paymentRepo.findByPgOrderId(pgOrderId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment", "pgOrderId", pgOrderId);
        }

        // Idempotency check: if already captured, skip
        if ("captured".equalsIgnoreCase(payment.getPgStatus())) {
            log.info("Payment {} already captured, skipping duplicate processing.", pgOrderId);
            return;
        }

        payment.setPgPaymentId(pgPaymentId);
        payment.setPgStatus("captured");
        paymentRepo.save(payment);

        OrderSummary order = orderProvider.getOrderSummary(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", payment.getOrderId()));

        if ("PAYMENT_CAPTURED".equalsIgnoreCase(order.currentStatus())) {
            return; // Already processed
        }

        // Record for Social Proof
        try {
            socialProofService.recordRecentPurchase(0L, "Someone"); // Simplified for decoupling
        } catch (Exception e) {
            log.warn("Failed to record social proof: {}", e.getMessage());
        }

        try {
            var event = new OrderPaidEvent(
                    order.orderId(),
                    order.email(),
                    order.totalAmount(),
                    pgPaymentId,
                    payment.getPaymentMethod());

            eventPublisher.publishEvent(event);

            // Unified stream for external systems
            eventProducer.publish("payment_events", event);

        } catch (Exception e) {
            log.error("Failed to publish payment events: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public String createInternalOrder(Long orderId) {
        var response = initiatePayment(orderId, "RAZORPAY");
        return response.pgOrderId();
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    public com.app.finance.payloads.PaymentInitResponse initiatePayment(Long orderId, String paymentMethod) {
        OrderSummary order = orderProvider.getOrderSummary(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        Payment payment = paymentRepo.findByOrderId(orderId).stream().findFirst().orElse(null);
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(orderId);
            payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "RAZORPAY");
            payment.setStatus("PENDING");
            payment.setAmount(order.totalAmount());
        }

        double totalAmount = order.totalAmount().doubleValue();
        double walletBalance = walletService.getBalance(order.email());
        double payableAmount = totalAmount;

        // Handle Wallet usage
        if ("WALLET".equalsIgnoreCase(payment.getPaymentMethod())
                || "SPLIT".equalsIgnoreCase(payment.getPaymentMethod())) {
            double walletToUse = Math.min(walletBalance, totalAmount);
            if (walletToUse > 0) {
                payment.setWalletAmount(java.math.BigDecimal.valueOf(walletToUse));
                payableAmount = totalAmount - walletToUse;

                // Debit wallet immediately
                walletService.debit(order.email(), walletToUse, "Payment for Order #" + orderId,
                        String.valueOf(orderId));
            }
        }

        // If fully paid by wallet
        if (payableAmount <= 0) {
            payment.setPgStatus("captured");
            payment.setPgOrderId("WALLET_ONLY");
            Payment saved = paymentRepo.save(payment);

            // Trigger capture logic
            processPaymentCapture("WALLET_ONLY", "WALLET_TXN_" + orderId);
            return new com.app.finance.payloads.PaymentInitResponse(saved.getPaymentId(), "WALLET_ONLY", "captured");
        }

        try {
            org.json.JSONObject orderRequest = new org.json.JSONObject();
            orderRequest.put("amount", (int) (payableAmount * 100)); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + orderId);

            com.razorpay.Order razorpayOrder = getRazorpayClient().orders.create(orderRequest);
            String pgOrderId = razorpayOrder.get("id");

            payment.setPgOrderId(pgOrderId);
            payment.setPgStatus("created");
            Payment saved = paymentRepo.save(payment);

            return new com.app.finance.payloads.PaymentInitResponse(saved.getPaymentId(), pgOrderId, "created");
        } catch (com.razorpay.RazorpayException e) {
            // Rollback wallet debit if PG order creation fails
            if (payment.getWalletAmount() != null && payment.getWalletAmount().doubleValue() > 0) {
                walletService.credit(order.email(), payment.getWalletAmount().doubleValue(),
                        "Order creation failed - Rollback",
                        String.valueOf(orderId));
                payment.setWalletAmount(java.math.BigDecimal.ZERO);
                paymentRepo.save(payment);
            }
            throw new com.app.core.APIException("Error creating Razorpay order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    @AuditTrail(action = "VERIFY_PAYMENT")
    public PaymentDTO verifyPayment(Long orderId, String paymentId, String signature) {
        Payment payment = paymentRepo.findByOrderId(orderId).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        if (payment.getPgOrderId() == null) {
            throw new RuntimeException("Payment initialization missing for order " + orderId);
        }

        String lockKey = "payment:verify:" + orderId;
        boolean locked = lockService.tryLock(lockKey, Duration.ofMinutes(2));

        if (!locked) {
            throw new RuntimeException("Payment verification already in progress for order " + orderId);
        }

        try {
            // Idempotency check: if already captured, return existing DTO
            if ("captured".equalsIgnoreCase(payment.getPgStatus())) {
                log.info("Payment for order {} already verified.", orderId);
                return paymentMapper.paymentToPaymentDTO(payment);
            }

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", payment.getPgOrderId());
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, credentialProvider.getKeySecret());

            if (isValid) {
                payment.setPgPaymentId(paymentId);
                payment.setPgSignature(signature);
                payment.setPgStatus("captured");

                paymentRepo.save(payment);

                // Trigger Capture logic to emit events
                processPaymentCapture(payment.getPgOrderId(), paymentId);

                return paymentMapper.paymentToPaymentDTO(payment);
            } else {
                throw new RuntimeException("Payment signature verification failed");
            }

        } catch (RazorpayException e) {
            throw new RuntimeException("Error verifying payment: " + e.getMessage());
        } finally {
            lockService.unlock(lockKey);
        }
    }

    @Override
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    @AuditTrail(action = "INITIATE_REFUND")
    @Transactional
    public Map<String, Object> initiateRefund(String paymentId, Long amount, String notes) {
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
            refundRequest.put("speed", "normal");

            com.razorpay.Refund pgRefund = getRazorpayClient().payments.refund(paymentId, refundRequest);

            // Track refund in DB for financial reconciliation
            Payment payment = paymentRepo.findByPgPaymentId(paymentId).orElse(null);
            if (payment != null) {
                com.app.finance.entities.Refund refund = new com.app.finance.entities.Refund();
                refund.setOrderId(payment.getOrderId());
                refund.setPgRefundId(pgRefund.get("id"));
                refund.setPgPaymentId(paymentId);
                refund.setAmount(Double.valueOf(pgRefund.get("amount").toString()) / 100.0);
                refund.setStatus(pgRefund.get("status"));
                refund.setReason(notes);
                refundRepo.save(refund);

                // Publish Refund Event
                eventPublisher.publishEvent(new OrderStatusEvent(
                        payment.getOrderId(),
                        "REFUND_INITIATED"));

                log.info("Refund {} initiated for order {}. Amount: {}", pgRefund.get("id"), payment.getOrderId(),
                        refund.getAmount());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("refund_id", pgRefund.get("id"));
            response.put("payment_id", pgRefund.get("payment_id"));
            response.put("amount", pgRefund.get("amount"));
            response.put("status", pgRefund.get("status"));

            return response;
        } catch (RazorpayException e) {
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void processRefundForOrder(Long orderId, String reason) {
        log.info("Processing refund logic for Order ID: {}", orderId);
        // Find successful payment for the order
        Payment payment = paymentRepo.findByOrderId(orderId).stream()
                .filter(p -> "captured".equalsIgnoreCase(p.getPgStatus()))
                .findFirst()
                .orElse(null);

        if (payment != null && payment.getPgPaymentId() != null) {
            log.info("Found captured payment {} for Order {}. Initiating refund...", payment.getPgPaymentId(), orderId);
            try {
                // Determine if it was wallet or PG
                if ("WALLET_ONLY".equals(payment.getPgOrderId())) {
                    // Full wallet refund
                    // Assuming amount is in Payment
                    if (payment.getWalletAmount() != null) { // or total amount
                        walletService.credit(getPaymentDetails(payment.getPgPaymentId()).get("email").toString(),
                                payment.getAmount().doubleValue(), // or walletAmount
                                "Refund for Order #" + orderId, String.valueOf(orderId));
                        // We need better handling for WALLET_ONLY refund, but initiateRefund is mainly
                        // for Razorpay
                        log.info("Wallet refund completed for Order {}", orderId);
                    }
                } else {
                    // Razorpay refund
                    initiateRefund(payment.getPgPaymentId(), null, reason);
                }
            } catch (Exception e) {
                log.error("Failed to process refund for Order {}: {}", orderId, e.getMessage());
            }
        } else {
            log.warn("No capture payment found for Order {}. Refund skipped.", orderId);
        }
    }

    @Override
    public Map<String, Object> getPaymentDetails(String paymentId) {
        try {
            com.razorpay.Payment payment = getRazorpayClient().payments.fetch(paymentId);

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
            throw new RuntimeException("Failed to fetch payment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isPaymentCompleted(Long orderId) {
        return paymentRepo.findByOrderId(orderId).stream()
                .anyMatch(p -> "captured".equalsIgnoreCase(p.getPgStatus()));
    }
}
