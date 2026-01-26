package com.app.discount.mappers;

import com.app.discount.entities.Coupon;
import com.app.discount.payloads.CouponDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponDTO toDTO(Coupon coupon);

    Coupon toEntity(CouponDTO couponDTO);
}
