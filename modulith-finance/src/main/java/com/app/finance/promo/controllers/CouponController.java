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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Coupons", description = "Promotion and Discount Coupon management")
@SecurityRequirement(name = "E-Commerce Application")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Create Coupon", description = "Adds a new discount coupon to the system. Restricted to ADMIN.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponDTO> createCoupon(@RequestBody CouponDTO couponDTO) {
        CouponDTO created = couponService.createCoupon(couponDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Active Coupons", description = "Retrieves all currently active and available coupons for customers.")
    @GetMapping("/active")
    public ResponseEntity<List<CouponDTO>> getAllActiveCoupons() {
        List<CouponDTO> coupons = couponService.getAllActiveCoupons();
        return ResponseEntity.ok(coupons);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Validate Coupon", description = "Checks if a coupon code is applicable to the current order amount and returns the discount details.")
    @GetMapping("/validate/{code}")
    public ResponseEntity<Map<String, Object>> validateCoupon(
            @io.swagger.v3.oas.annotations.Parameter(description = "The coupon code string") @PathVariable String code,
            @io.swagger.v3.oas.annotations.Parameter(description = "The current order subtotal") @RequestParam java.math.BigDecimal orderAmount) {

        CouponDTO coupon = couponService.validateCoupon(code, orderAmount);
        java.math.BigDecimal discount = couponService.calculateDiscount(code, orderAmount);

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "coupon", coupon,
                "discountAmount", discount,
                "finalAmount", orderAmount.subtract(discount)));
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Coupon Details", description = "Fetches details of a specific coupon by its code.")
    @GetMapping("/{code}")
    public ResponseEntity<CouponDTO> getCouponByCode(@PathVariable String code) {
        CouponDTO coupon = couponService.getCouponByCode(code);
        return ResponseEntity.ok(coupon);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Update Coupon", description = "Modifies existing coupon parameters. Restricted to ADMIN.")
    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponDTO> updateCoupon(@PathVariable String code,
            @RequestBody CouponDTO couponDTO) {
        CouponDTO updated = couponService.updateCoupon(code, couponDTO);
        return ResponseEntity.ok(updated);
    }

    @io.swagger.v3.oas.annotations.Operation(summary = "Delete Coupon", description = "Removes a coupon from the system. Restricted to ADMIN.")
    @DeleteMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCoupon(@PathVariable String code) {
        couponService.deleteCoupon(code);
        return ResponseEntity.ok(Map.of("message", "Coupon deleted successfully"));
    }
}
