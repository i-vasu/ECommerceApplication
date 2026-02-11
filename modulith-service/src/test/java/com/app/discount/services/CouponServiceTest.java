package com.app.discount.services;

import com.app.finance.promo.entities.Coupon;
import com.app.finance.promo.mappers.CouponMapper;
import com.app.finance.promo.payloads.CouponDTO;
import com.app.finance.promo.repositories.CouponRepo;
import com.app.finance.promo.services.CouponServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepo couponRepo;

    @Mock
    private CouponMapper couponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon testCoupon;
    private CouponDTO testCouponDTO;

    @BeforeEach
    void setUp() {
        testCoupon = new Coupon();
        testCoupon.setCouponId(1L);
        testCoupon.setCode("SAVE20");
        testCoupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        testCoupon.setDiscountValue(java.math.BigDecimal.valueOf(20.0));
        testCoupon.setMinOrderAmount(java.math.BigDecimal.valueOf(100.0));
        testCoupon.setMaxDiscountAmount(java.math.BigDecimal.valueOf(50.0));
        testCoupon.setValidFrom(LocalDateTime.now().minusDays(1));
        testCoupon.setValidTo(LocalDateTime.now().plusDays(30));
        testCoupon.setUsageLimit(100);
        testCoupon.setUsedCount(10);
        testCoupon.setActive(true);

        testCouponDTO = new CouponDTO(1L, "SAVE20", null, Coupon.DiscountType.PERCENTAGE, 
                java.math.BigDecimal.valueOf(20.0), java.math.BigDecimal.valueOf(100.0), java.math.BigDecimal.valueOf(50.0), 
                null, null, 100, 10, null, null, true);
    }

    @Test
    void testCalculateDiscount_Percentage() {
        when(couponRepo.findActiveByCode(eq("SAVE20"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testCoupon));
        when(couponMapper.toDTO(testCoupon)).thenReturn(testCouponDTO);

        java.math.BigDecimal discount = couponService.calculateDiscount("SAVE20", java.math.BigDecimal.valueOf(200.0));

        org.assertj.core.api.Assertions.assertThat(discount).isEqualByComparingTo(java.math.BigDecimal.valueOf(40.0)); // 20% of 200
        verify(couponRepo).findActiveByCode(eq("SAVE20"), any(LocalDateTime.class));
    }

    @Test
    void testCalculateDiscount_WithMaxCap() {
        when(couponRepo.findActiveByCode(eq("SAVE20"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testCoupon));
        when(couponMapper.toDTO(testCoupon)).thenReturn(testCouponDTO);

        java.math.BigDecimal discount = couponService.calculateDiscount("SAVE20", java.math.BigDecimal.valueOf(500.0));

        org.assertj.core.api.Assertions.assertThat(discount).isEqualByComparingTo(java.math.BigDecimal.valueOf(50.0)); // Capped at maxDiscountAmount
    }

    @Test
    void testCalculateDiscount_FixedAmount() {
        testCoupon.setDiscountType(Coupon.DiscountType.FIXED_AMOUNT);
        testCoupon.setDiscountValue(java.math.BigDecimal.valueOf(30.0));

        CouponDTO fixedCouponDTO = new CouponDTO(1L, "SAVE20", null, Coupon.DiscountType.FIXED_AMOUNT, 
                java.math.BigDecimal.valueOf(30.0), java.math.BigDecimal.valueOf(100.0), java.math.BigDecimal.valueOf(50.0), 
                null, null, 100, 10, null, null, true);

        when(couponRepo.findActiveByCode(eq("SAVE20"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testCoupon));
        when(couponMapper.toDTO(testCoupon)).thenReturn(fixedCouponDTO);

        java.math.BigDecimal discount = couponService.calculateDiscount("SAVE20", java.math.BigDecimal.valueOf(200.0));

        org.assertj.core.api.Assertions.assertThat(discount).isEqualByComparingTo(java.math.BigDecimal.valueOf(30.0));
    }
}
