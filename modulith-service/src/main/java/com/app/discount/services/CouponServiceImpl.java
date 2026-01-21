package com.app.discount.services;

import com.app.discount.entities.Coupon;
import com.app.discount.entities.CouponUsage;
import com.app.discount.payloads.CouponDTO;
import com.app.discount.repositories.CouponRepo;
import com.app.discount.repositories.CouponUsageRepo;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import com.app.discount.mappers.CouponMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepo couponRepo;
    private final CouponUsageRepo couponUsageRepo;
    private final CouponMapper couponMapper;

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
    public CouponDTO validateCoupon(String code, Double orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = couponRepo.findActiveByCode(code, now)
                .orElseThrow(() -> new APIException("Invalid or expired coupon code: " + code));

        // Check usage limit
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new APIException("Coupon usage limit exceeded");
        }

        // Check minimum order amount
        if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            throw new APIException("Minimum order amount not met. Required: " + coupon.getMinOrderAmount());
        }

        return couponMapper.toDTO(coupon);
    }

    @Override
    public Double calculateDiscount(String code, Double orderAmount) {
        CouponDTO coupon = validateCoupon(code, orderAmount);

        double discount = 0.0;

        if (coupon.discountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = (orderAmount * coupon.discountValue()) / 100.0;
        } else if (coupon.discountType() == Coupon.DiscountType.FIXED_AMOUNT) {
            discount = coupon.discountValue();
        }

        // Apply max discount cap if set
        if (coupon.maxDiscountAmount() != null && discount > coupon.maxDiscountAmount()) {
            discount = coupon.maxDiscountAmount();
        }

        // Ensure discount doesn't exceed order amount
        if (discount > orderAmount) {
            discount = orderAmount;
        }

        return discount;
    }

    @Override
    @Transactional
    public void applyCoupon(String code, Long userId, Long orderId, Double discountApplied) {
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

        // Increment usage count
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepo.save(coupon);
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
}
