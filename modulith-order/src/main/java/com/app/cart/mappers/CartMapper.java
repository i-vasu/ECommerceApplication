package com.app.cart.mappers;

import com.app.order.entities.Cart;
import com.app.cart.payloads.CartDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    CartDTO cartToCartDTO(Cart cart);

    Cart cartDTOToCart(CartDTO cartDTO);
}
