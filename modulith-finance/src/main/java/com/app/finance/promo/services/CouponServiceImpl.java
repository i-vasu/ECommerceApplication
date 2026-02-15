package com.app.finance.promo.services;

import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.finance.promo.CouponValidationService;
import com.app.finance.promo.entities.Coupon;
import com.app.finance.promo.entities.CouponUsage;
import com.app.finance.promo.mappers.CouponMapper;
import com.app.finance.promo.payloads.CouponDTO;
import com.app.finance.promo.repositories.CouponRepo;
import com.app.finance.promo.repositories.CouponUsageRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService, CouponValidationService {

    private final CouponRepo couponRepo;
    private final CouponUsageRepo couponUsageRepo;
    private final CouponMapper couponMapper;

    @Override
    public CouponDiscount validateAndCalculate(String couponCode, java.math.BigDecimal subtotal) {
        try {
            java.math.BigDecimal discount = calculateDiscount(couponCode, subtotal);
            return new CouponDiscount(discount, couponCode);
        } catch (Exception e) {
            // Return zero discount if validation fails, with the message
            return new CouponDiscount(java.math.BigDecimal.ZERO, couponCode);
        }
    }

    @Override
    @Transactional
    public CouponDTO createCoupon(CouponDTO couponDTO) {
        // Check if code already exists
        if (couponRepo.findByCode(couponDTO.code()).isPresent()) {
            throw new APIException("Coupon code already exists: " + couponDTO.code());
        }

        Coupon coupon = couponMapper.toEntity(couponDTO);
        coupon.setUsedCount(0);
        coupon.setCreatedAt(LocalDateTime.now());

        Coupon savedCoupon = couponRepo.save(coupon);
        return couponMapper.toDTO(savedCoupon);
    }

    @Override
    public CouponDTO validateCoupon(String code, java.math.BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = couponRepo.findActiveByCode(code, now)
                .orElseThrow(() -> new APIException("Invalid or expired coupon code: " + code));

        // Check usage limit
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new APIException("Coupon usage limit exceeded");
        }

        // GAP-10: Check per-user usage limit
        if (coupon.getMaxUsesPerUser() != null) {
            // Fetch current user from SecurityContext
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Long currentUserId = null;
            if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwt) {
                Object userIdClaim = jwt.getTokenAttributes().get("user_id");
                if (userIdClaim instanceof Number n) {
                    currentUserId = n.longValue();
                }
            }
            
            if (currentUserId != null) {
                long userUsageCount = couponUsageRepo.countByCouponCodeAndUserId(code, currentUserId);
                if (userUsageCount >= coupon.getMaxUsesPerUser()) {
                    throw new APIException("You have reached the maximum usage limit for this coupon.");
                }
            }
        }

        // Check minimum order amount
        if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new APIException("Minimum order amount not met. Required: " + coupon.getMinOrderAmount());
        }

        return couponMapper.toDTO(coupon);
    }

    @Override
    public java.math.BigDecimal calculateDiscount(String code, java.math.BigDecimal orderAmount) {
        CouponDTO coupon = validateCoupon(code, orderAmount);

        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;

        if (coupon.discountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.discountValue()).divide(java.math.BigDecimal.valueOf(100), java.math.RoundingMode.HALF_UP);
        } else if (coupon.discountType() == Coupon.DiscountType.FIXED_AMOUNT) {
            discount = coupon.discountValue();
        }

        // Apply max discount cap if set
        if (coupon.maxDiscountAmount() != null && discount.compareTo(coupon.maxDiscountAmount()) > 0) {
            discount = coupon.maxDiscountAmount();
        }

        // Ensure discount doesn't exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    @Override
    @Transactional
    public void applyCoupon(String code, Long userId, Long orderId, java.math.BigDecimal discountApplied) {
        Coupon coupon = couponRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));

        // Record usage
        CouponUsage usage = new CouponUsage();
        usage.setCoupon(coupon);
        usage.setUserId(userId);
        usage.setOrderId(orderId);
        usage.setDiscountApplied(discountApplied);
        usage.setUsedAt(LocalDateTime.now());

        couponUsageRepo.save(usage);

        // GAP-11: Atomic usage count increment
        int updated = couponRepo.incrementUsedCountAtomic(coupon.getCouponId());
        if (updated == 0) {
            throw new APIException("Coupon usage limit exceeded during allocation.");
        }
    }

    @Override
    public List<CouponDTO> getAllActiveCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponRepo.findAllActiveCoupons(now).stream()
                .map(couponMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CouponDTO getCouponByCode(String code) {
        Coupon coupon = couponRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));
        return couponMapper.toDTO(coupon);
    }

    @Override
    @Transactional
    public void incrementUsageCount(Long couponId) {
        Coupon coupon = couponRepo.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "couponId", couponId));
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepo.save(coupon);
    }

    @Override
    @Transactional
    public CouponDTO updateCoupon(String code, CouponDTO couponDTO) {
        Coupon coupon = couponRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));

        // Update mutable fields
        if (couponDTO.discountType() != null) coupon.setDiscountType(couponDTO.discountType());
        if (couponDTO.discountValue() != null) coupon.setDiscountValue(couponDTO.discountValue());
        if (couponDTO.minOrderAmount() != null) coupon.setMinOrderAmount(couponDTO.minOrderAmount());
        if (couponDTO.maxDiscountAmount() != null) coupon.setMaxDiscountAmount(couponDTO.maxDiscountAmount());
        if (couponDTO.usageLimit() != null) coupon.setUsageLimit(couponDTO.usageLimit());
        if (couponDTO.validFrom() != null) coupon.setValidFrom(couponDTO.validFrom());
        if (couponDTO.validTo() != null) coupon.setValidTo(couponDTO.validTo());
        coupon.setActive(couponDTO.active());

        return couponMapper.toDTO(couponRepo.save(coupon));
    }

    @Override
    @Transactional
    public void deleteCoupon(String code) {
        Coupon coupon = couponRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));
        couponRepo.delete(coupon);
    }
}
