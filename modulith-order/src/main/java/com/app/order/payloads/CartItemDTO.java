package com.app.order.payloads;

import com.app.cart.payloads.CartDTO;
import com.app.catalog.payloads.ProductDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

	private Long cartItemId;
	private CartDTO cart;
	private ProductDTO product;
	private Integer quantity;
	private java.math.BigDecimal discount;
	private java.math.BigDecimal productPrice;
}
