package com.app.finance.promo.mappers;

import com.app.finance.promo.entities.Coupon;
import com.app.finance.promo.payloads.CouponDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponDTO toDTO(Coupon coupon);

    Coupon toEntity(CouponDTO couponDTO);
}
