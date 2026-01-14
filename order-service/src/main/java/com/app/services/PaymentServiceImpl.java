package com.app.services;

import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.entites.Order;
import com.app.entites.Payment;
import com.app.exceptions.ResourceNotFoundException;
import com.app.payloads.PaymentDTO;
import com.app.repositories.OrderRepo;
import com.app.repositories.PaymentRepo;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private ModelMapper modelMapper;

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
            String secret = "your_key_secret"; // This should ideally come from config/bean, but RazorpayClient has it.
            // Wait, Utils.verifyPaymentSignature needs the secret string.
            // I can inject it from properties or get it from a bean if I exposed it.
            // For now I'll check how to get it from RazorpayClient or inject value.

            // Re-injecting value to be safe as RazorpayClient doesn't expose getter for
            // secret easily?
            // Actually I should Inject it.

            // Let's assume validation passes for now or use a placeholder and fix it in
            // next step with @Value
            // But for correctness I will add @Value

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", payment.getPgOrderId());
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            // I will fix the secret injection in the actual code block below.

            boolean isValid = Utils.verifyPaymentSignature(options, getSecret());

            if (isValid) {
                payment.setPgPaymentId(paymentId);
                payment.setPgSignature(signature);
                payment.setPgStatus("captured");

                paymentRepo.save(payment);

                return modelMapper.map(payment, PaymentDTO.class);
            } else {
                throw new RuntimeException("Payment signature verification failed");
            }

        } catch (RazorpayException e) {
            throw new RuntimeException("Error verifying payment: " + e.getMessage());
        }
    }

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String secret;

    private String getSecret() {
        return secret;
    }

}
