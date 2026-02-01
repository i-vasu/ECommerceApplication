package com.app.catalog.entities;

import com.app.catalog.review.entities.ProductReview;
import com.app.core.persistence.ExtensibleEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product extends ExtensibleEntity {
	@jakarta.persistence.Version
	private Long version;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long productId;

	@NotBlank
	@Size(min = 3, message = "Product name must contain atleast 3 characters")
	private String productName;

	private String itemCode;
	private String image;

	@NotBlank
	@Size(min = 6, message = "Product description must contain atleast 6 characters")
	private String description;

	private Integer quantity;
	private BigDecimal price;
	private BigDecimal discount;
	private BigDecimal specialPrice;
	private String brand;

	    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @jakarta.persistence.Column(name = "tag")
    private List<String> tags;

	@ManyToOne
	@JoinColumn(name = "category_id")
	private Category category;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductVariant> variants = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductMedia> media = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProductReview> reviews = new ArrayList<>();

	private boolean isCustomizable = false;

	@jakarta.persistence.Transient
	private float[] dbEmbedding;

	@CreationTimestamp
	@jakarta.persistence.Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	private boolean isBundle = false;

	@OneToMany(mappedBy = "bundleProduct", cascade = CascadeType.ALL)
	private List<BundleItem> bundleItems = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
	private List<ProductPriceList> priceLists = new ArrayList<>();

	@jakarta.persistence.Column(name = "quality_score")
	private Double qualityScore = 1.0;

	public Product() {
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

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(BigDecimal discount) {
		this.discount = discount;
	}

	public BigDecimal getSpecialPrice() {
		return specialPrice;
	}

	public void setSpecialPrice(BigDecimal specialPrice) {
		this.specialPrice = specialPrice;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public List<ProductVariant> getVariants() {
		return variants;
	}

	public void setVariants(List<ProductVariant> variants) {
		this.variants = variants;
	}

	public List<ProductMedia> getMedia() {
		return media;
	}

	public void setMedia(List<ProductMedia> media) {
		this.media = media;
	}

	public List<ProductReview> getReviews() {
		return reviews;
	}

	public void setReviews(List<ProductReview> reviews) {
		this.reviews = reviews;
	}

	public boolean isCustomizable() {
		return isCustomizable;
	}

	public void setCustomizable(boolean customizable) {
		isCustomizable = customizable;
	}

	public float[] getDbEmbedding() {
		return dbEmbedding;
	}

	public void setDbEmbedding(float[] dbEmbedding) {
		this.dbEmbedding = dbEmbedding;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isBundle() {
		return isBundle;
	}

	public void setBundle(boolean bundle) {
		isBundle = bundle;
	}

	public List<BundleItem> getBundleItems() {
		return bundleItems;
	}

	public void setBundleItems(List<BundleItem> bundleItems) {
		this.bundleItems = bundleItems;
	}

	public List<ProductPriceList> getPriceLists() {
		return priceLists;
	}

	public void setPriceLists(List<ProductPriceList> priceLists) {
		this.priceLists = priceLists;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public List<String> getTags() {
		return tags;
	}

	public Double getQualityScore() {
		return qualityScore;
	}

	public void setQualityScore(Double qualityScore) {
		this.qualityScore = qualityScore;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
