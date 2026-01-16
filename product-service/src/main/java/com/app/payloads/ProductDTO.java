package com.app.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

	private Long productId;
	private String productName;
	private String itemCode;
	private String image;
	private String description;
	private Integer quantity;
	private double price;
	private double discount;
	private double specialPrice;

	private java.util.List<ProductVariantDTO> variants = new java.util.ArrayList<>();
	private java.util.List<ProductMediaDTO> media = new java.util.ArrayList<>();
	private java.util.List<ProductReviewDTO> reviews = new java.util.ArrayList<>();

}
