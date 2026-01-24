package com.app.order.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.app.cart.payloads.CartDTO;
import com.app.product.payloads.ProductDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

	private Long cartItemId;
	private CartDTO cart;
	private ProductDTO product;
	private Integer quantity;
	private double discount;
	private double productPrice;
}
