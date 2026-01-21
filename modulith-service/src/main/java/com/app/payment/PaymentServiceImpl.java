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
import com.app.order.payloads.OrderPaidEvent;
import com.app.payment.mappers.PaymentMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.core.async.EventProducer;
import com.app.commerce.states.OrderStatus;

@Service
public class PaymentServiceImpl implements PaymentService {

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String secret;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventProducer eventProducer;

    @Override
    @Transactional
    public void processPaymentCapture(String pgOrderId, String pgPaymentId) {
        Payment payment = paymentRepo.findByPgOrderId(pgOrderId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment", "pgOrderId", pgOrderId);
        }

        payment.setPgPaymentId(pgPaymentId);
        payment.setPgStatus("captured");
        paymentRepo.save(payment);

        Order order = payment.getOrder();
        order.setOrderStatus(OrderStatus.PAYMENT_CAPTURED);
        orderRepo.save(order);

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
    public String createInternalOrder(Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in paisa (100 * amount)
            orderRequest.put("amount", (int) (order.getTotalAmount() * 100)); // Casting to int might lose precision for
                                                                              // very large nums, but standard for
                                                                              // Double to int (paisa)
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + orderId);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String pgOrderId = razorpayOrder.get("id");

            Payment payment = order.getPayment();
            if (payment == null) {
                payment = new Payment();
                payment.setOrder(order);
                order.setPayment(payment);
            }

            payment.setPgOrderId(pgOrderId);
            payment.setPgStatus("created");
            payment.setPaymentMethod("RAZORPAY");

            paymentRepo.save(payment);

            // If not cascaded properly, might need to save order too, but usually
            // unnecessary if payment is saved and linked.
            // Checking Order entity: CascadeType.PERSIST, CascadeType.MERGE on payment.

            return pgOrderId;

        } catch (RazorpayException e) {
            throw new RuntimeException("Error creating Razorpay order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentDTO verifyPayment(Long orderId, String paymentId, String signature) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        Payment payment = order.getPayment();
        if (payment == null || payment.getPgOrderId() == null) {
            throw new RuntimeException("Payment initialization missing for order " + orderId);
        }

        try {
            // Validate signature using Razorpay Utils
            // The secret is injected via @Value("${razorpay.key.secret}")
            if (this.secret == null || this.secret.isEmpty()) {
                throw new RuntimeException("Razorpay secret not configured");
            }

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", payment.getPgOrderId());
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, this.secret);

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
        }
    }

    @Override
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

            com.razorpay.Refund refund = razorpayClient.payments.refund(paymentId, refundRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("refund_id", refund.get("id"));
            response.put("payment_id", refund.get("payment_id"));
            response.put("amount", refund.get("amount"));
            response.put("status", refund.get("status"));
            response.put("speed_requested", refund.get("speed_requested"));

            return response;
        } catch (RazorpayException e) {
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getPaymentDetails(String paymentId) {
        try {
            com.razorpay.Payment payment = razorpayClient.payments.fetch(paymentId);

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
