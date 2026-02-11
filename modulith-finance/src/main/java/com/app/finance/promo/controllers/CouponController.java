package com.app.finance.promo.controllers;

import com.app.finance.promo.payloads.CouponDTO;
import com.app.finance.promo.services.CouponService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/coupons")
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponDTO> createCoupon(@RequestBody CouponDTO couponDTO) {
        CouponDTO created = couponService.createCoupon(couponDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/active")
    public ResponseEntity<List<CouponDTO>> getAllActiveCoupons() {
        List<CouponDTO> coupons = couponService.getAllActiveCoupons();
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<Map<String, Object>> validateCoupon(
            @PathVariable String code,
            @RequestParam java.math.BigDecimal orderAmount) {

        CouponDTO coupon = couponService.validateCoupon(code, orderAmount);
        java.math.BigDecimal discount = couponService.calculateDiscount(code, orderAmount);

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "coupon", coupon,
                "discountAmount", discount,
                "finalAmount", orderAmount.subtract(discount)));
    }

    @GetMapping("/{code}")
    public ResponseEntity<CouponDTO> getCouponByCode(@PathVariable String code) {
        CouponDTO coupon = couponService.getCouponByCode(code);
        return ResponseEntity.ok(coupon);
    }
}
