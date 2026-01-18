package com.app.product.payloads;

import com.app.review.payloads.ProductReviewDTO;

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

	public ProductDTO() {
	}

	public ProductDTO(Long productId, String productName, String itemCode, String image, String description,
			Integer quantity, double price, double discount, double specialPrice,
			java.util.List<ProductVariantDTO> variants, java.util.List<ProductMediaDTO> media,
			java.util.List<ProductReviewDTO> reviews) {
		this.productId = productId;
		this.productName = productName;
		this.itemCode = itemCode;
		this.image = image;
		this.description = description;
		this.quantity = quantity;
		this.price = price;
		this.discount = discount;
		this.specialPrice = specialPrice;
		this.variants = variants;
		this.media = media;
		this.reviews = reviews;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public double getDiscount() {
		return discount;
	}

	public void setDiscount(double discount) {
		this.discount = discount;
	}

	public double getSpecialPrice() {
		return specialPrice;
	}

	public void setSpecialPrice(double specialPrice) {
		this.specialPrice = specialPrice;
	}

	public java.util.List<ProductVariantDTO> getVariants() {
		return variants;
	}

	public void setVariants(java.util.List<ProductVariantDTO> variants) {
		this.variants = variants;
	}

	public java.util.List<ProductMediaDTO> getMedia() {
		return media;
	}

	public void setMedia(java.util.List<ProductMediaDTO> media) {
		this.media = media;
	}

	public java.util.List<ProductReviewDTO> getReviews() {
		return reviews;
	}

	public void setReviews(java.util.List<ProductReviewDTO> reviews) {
		this.reviews = reviews;
	}
}
