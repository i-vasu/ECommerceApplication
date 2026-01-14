package com.app.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.payloads.APIResponse;
import com.app.payloads.PaymentDTO;
import com.app.services.PaymentService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create/{orderId}")
    public ResponseEntity<APIResponse> createOrder(@PathVariable Long orderId) {
        String razorpayOrderId = paymentService.createInternalOrder(orderId);
        // Returning the Razorpay Order ID in the message for simplicity, or could wrap
        // in a proper DTO
        return new ResponseEntity<APIResponse>(new APIResponse(razorpayOrderId, true), HttpStatus.OK);
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentDTO> verifyPayment(@RequestParam Long orderId, @RequestParam String paymentId,
            @RequestParam String signature) {
        PaymentDTO paymentDTO = paymentService.verifyPayment(orderId, paymentId, signature);
        return new ResponseEntity<PaymentDTO>(paymentDTO, HttpStatus.OK);
    }

}
