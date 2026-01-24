package com.app.payment;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.order.entities.Order;
import com.app.order.entities.Payment;
import com.app.core.ResourceNotFoundException;
import com.app.order.payloads.PaymentDTO;
import com.app.order.repositories.PaymentRepo;
import com.app.order.repositories.OrderRepo;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.app.core.events.OrderPaidEvent;
import com.app.payment.mappers.PaymentMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.core.async.EventProducer;
import com.app.core.audit.AuditTrail;
import com.app.commerce.states.OrderStatus;
import com.app.identity.services.WalletService;
import com.app.core.APIException;
import com.app.core.multitenancy.RazorpayCredentialProvider;
import com.app.review.services.SocialProofService;
import com.app.order.entities.Refund;
import com.app.order.repositories.RefundRepo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceImpl.class);

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
    private OrderRepo orderRepo;

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
    private RefundRepo refundRepo;

    @Autowired
    private com.app.core.services.RedisLockService lockService;

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

        Order order = payment.getOrder();
        if (order.getOrderStatus() == OrderStatus.PAYMENT_CAPTURED) {
            return; // Already processed
        }

        order.setOrderStatus(OrderStatus.PAYMENT_CAPTURED);
        orderRepo.save(order);

        // Record for Social Proof
        try {
            String location = "India"; // Default
            if (!order.getEmail().isEmpty()) {
                // Try to get user city for "Social Proof"
                // This is a naive way for PoC, ideally location is on the order/shipment
                location = "Someone";
            }

            for (var item : order.getOrderItems()) {
                socialProofService.recordRecentPurchase(item.getProduct().getProductId(), location);
            }
        } catch (Exception e) {
            log.warn("Failed to record social proof: {}", e.getMessage());
        }

        // Publish Events for Async Workflows (Replacing RabbitMQ with DragonflyDB
        // Stream)

        try {
            OrderPaidEvent event = new OrderPaidEvent(
                    order.getOrderId(),
                    order.getEmail(),
                    order.getTotalAmount(),
                    pgPaymentId);

            eventProducer.publish("payment_events", event); // Unified stream for Payment/Order Events?
            // Wait, previous code used "order-events". StreamConfig used "orders_stream".
            // I should stick to constants in RedisStreamConfig if possible, or just string
            // literals consistent with plan.
            // Plan said "payment_success_stream" or shared.
            // Let's use "payment_events" for clarity, or "orders_stream" if we want unified
            // timeline.
            // NotificationConsumer listens to "orders_stream" (Order Placed).
            // It should also listen to "payment_events".

            // I'll use "payment_events". I need to update RedisStreamConfig to listen to
            // this too?
            // NotificationConsumer logic (Line 23) checked `if
            // (streamKey.contains("orders_stream"))`.
            // I'll update NotificationConsumer to handle payment events too.
            // For now, publish to "payment_events".

        } catch (Exception e) {
            System.err.println("Failed to publish payment event: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    public String createInternalOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            order.setPayment(payment);
        }

        double totalAmount = order.getTotalAmount();
        double walletBalance = walletService.getBalance(order.getEmail());
        double payableAmount = totalAmount;

        // Handle Wallet usage
        if ("WALLET".equalsIgnoreCase(payment.getPaymentMethod())
                || "SPLIT".equalsIgnoreCase(payment.getPaymentMethod())) {
            double walletToUse = Math.min(walletBalance, totalAmount);
            if (walletToUse > 0) {
                payment.setWalletAmount(walletToUse);
                payableAmount = totalAmount - walletToUse;

                // Debit wallet immediately
                walletService.debit(order.getEmail(), walletToUse, "Payment for Order #" + orderId,
                        String.valueOf(orderId));
            }
        }

        // If fully paid by wallet
        if (payableAmount <= 0) {
            payment.setPgStatus("captured");
            payment.setPgOrderId("WALLET_ONLY");
            paymentRepo.save(payment);

            // Trigger capture logic
            processPaymentCapture("WALLET_ONLY", "WALLET_TXN_" + orderId);
            return "WALLET_ONLY";
        }

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (payableAmount * 100));
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + orderId);

            com.razorpay.Order razorpayOrder = getRazorpayClient().orders.create(orderRequest);
            String pgOrderId = razorpayOrder.get("id");

            payment.setPgOrderId(pgOrderId);
            payment.setPgStatus("created");
            paymentRepo.save(payment);

            return pgOrderId;
        } catch (RazorpayException e) {
            // Rollback wallet debit if PG order creation fails
            if (payment.getWalletAmount() > 0) {
                walletService.credit(order.getEmail(), payment.getWalletAmount(), "Order creation failed - Rollback",
                        String.valueOf(orderId));
                payment.setWalletAmount(0.0);
                paymentRepo.save(payment);
            }
            throw new APIException("Error creating Razorpay order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "razorpay")
    @Retry(name = "razorpay")
    @AuditTrail(action = "VERIFY_PAYMENT")
    public PaymentDTO verifyPayment(Long orderId, String paymentId, String signature) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        Payment payment = order.getPayment();
        if (payment == null || payment.getPgOrderId() == null) {
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
            Payment payment = paymentRepo.findByPgPaymentId(paymentId);
            if (payment != null) {
                Refund refund = new Refund();
                refund.setOrder(payment.getOrder());
                refund.setPgRefundId(pgRefund.get("id"));
                refund.setPgPaymentId(paymentId);
                refund.setAmount(Double.valueOf(pgRefund.get("amount").toString()) / 100.0);
                refund.setStatus(pgRefund.get("status"));
                refund.setReason(notes);
                refundRepo.save(refund);

                Order order = payment.getOrder();
                order.setOrderStatus(OrderStatus.REFUND_INITIATED);
                orderRepo.save(order);

                log.info("Refund {} initiated for order {}. Amount: {}", pgRefund.get("id"), order.getOrderId(),
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

}
