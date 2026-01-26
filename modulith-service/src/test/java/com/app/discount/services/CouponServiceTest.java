package com.app.discount.services;

import com.app.discount.entities.Coupon;
import com.app.discount.payloads.CouponDTO;
import com.app.discount.repositories.CouponRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.app.discount.mappers.CouponMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        testCoupon.setDiscountValue(20.0);
        testCoupon.setMinOrderAmount(100.0);
        testCoupon.setMaxDiscountAmount(50.0);
        testCoupon.setValidFrom(LocalDateTime.now().minusDays(1));
        testCoupon.setValidTo(LocalDateTime.now().plusDays(30));
        testCoupon.setUsageLimit(100);
        testCoupon.setUsedCount(10);
        testCoupon.setActive(true);

        testCouponDTO = new CouponDTO(1L, "SAVE20", null, Coupon.DiscountType.PERCENTAGE, 20.0, 100.0, 50.0, null, null,
                100, 10, null, true);
    }

    @Test
    void testCalculateDiscount_Percentage() {
        when(couponRepo.findActiveByCode(eq("SAVE20"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testCoupon));
        when(couponMapper.toDTO(testCoupon)).thenReturn(testCouponDTO);

        Double discount = couponService.calculateDiscount("SAVE20", 200.0);

        assertEquals(40.0, discount); // 20% of 200
        verify(couponRepo).findActiveByCode(eq("SAVE20"), any(LocalDateTime.class));
    }

    @Test
    void testCalculateDiscount_WithMaxCap() {
        when(couponRepo.findActiveByCode(eq("SAVE20"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testCoupon));
        when(couponMapper.toDTO(testCoupon)).thenReturn(testCouponDTO);

        Double discount = couponService.calculateDiscount("SAVE20", 500.0);

        assertEquals(50.0, discount); // Capped at maxDiscountAmount
    }

    @Test
    void testCalculateDiscount_FixedAmount() {
        testCoupon.setDiscountType(Coupon.DiscountType.FIXED_AMOUNT);
        testCoupon.setDiscountValue(30.0);

        CouponDTO fixedCouponDTO = new CouponDTO(1L, "SAVE20", null, Coupon.DiscountType.FIXED_AMOUNT, 30.0, 100.0,
                50.0, null, null, 100, 10, null, true);

        when(couponRepo.findActiveByCode(eq("SAVE20"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testCoupon));
        when(couponMapper.toDTO(testCoupon)).thenReturn(fixedCouponDTO);

        Double discount = couponService.calculateDiscount("SAVE20", 200.0);

        assertEquals(30.0, discount);
    }
}
