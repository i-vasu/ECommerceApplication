package com.app.finance.promo.mappers;

import com.app.finance.promo.entities.Coupon;
import com.app.finance.promo.payloads.CouponDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponDTO toDTO(Coupon coupon);

    @Mapping(target = "createdAt", ignore = true)
    Coupon toEntity(CouponDTO couponDTO);
}
