package com.app.cart.mappers;

import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.payloads.CartDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @org.mapstruct.Mapping(target = "items", source = "cartItems")
    CartDTO cartToCartDTO(Cart cart);

    @org.mapstruct.Mapping(target = "cartItems", source = "items")
    Cart cartDTOToCart(CartDTO cartDTO);

    CartDTO.CartItemDTO cartItemToDTO(CartItem item);
}
