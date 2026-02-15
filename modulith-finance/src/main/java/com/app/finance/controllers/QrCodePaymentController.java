package com.app.finance.controllers;

import com.app.finance.payment.RazorpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * QR Code Payment Controller
 * Handles UPI QR code generation for quick payments
 */
@RestController
@RequestMapping("/api/v1/payments/qr")
@RequiredArgsConstructor
public class QrCodePaymentController {

    private final RazorpayService razorpayService;

    /**
     * Create QR code for specific amount (single-use)
     * Use case: Order-specific QR codes
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createQrCode(
            @RequestParam double amountInRupees,
            @RequestParam String description,
            @RequestParam(required = false) String customerId) {
        
        long amountInPaise = Math.round(amountInRupees * 100);
        Map<String, Object> qrCode = razorpayService.createQrCode(amountInPaise, description, customerId);
        
        return ResponseEntity.ok(qrCode);
    }

    /**
     * Create dynamic QR code (reusable, no fixed amount)
     * Use case: Store counter, in-store payments
     */
    @PostMapping("/create-dynamic")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    public ResponseEntity<Map<String, Object>> createDynamicQrCode(
            @RequestParam String description) {
        
        Map<String, Object> qrCode = razorpayService.createDynamicQrCode(description);
        return ResponseEntity.ok(qrCode);
    }

    /**
     * Get QR code details and payment status
     */
    @GetMapping("/{qrCodeId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getQrCodeDetails(@PathVariable String qrCodeId) {
        Map<String, Object> details = razorpayService.getQrCodeDetails(qrCodeId);
        return ResponseEntity.ok(details);
    }

    /**
     * Close/deactivate a QR code
     */
    @PostMapping("/{qrCodeId}/close")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CASHIER')")
    public ResponseEntity<Map<String, Object>> closeQrCode(@PathVariable String qrCodeId) {
        Map<String, Object> response = razorpayService.closeQrCode(qrCodeId);
        return ResponseEntity.ok(response);
    }
}
