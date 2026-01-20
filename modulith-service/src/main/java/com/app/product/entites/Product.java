package com.app.product.entites;

import java.util.ArrayList;
import java.util.List;
import com.app.review.entities.ProductReview;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

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
	private double price;
	private double discount;
	private double specialPrice;

	private String brand;

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

    // PGVector embedding is managed via VisualSearchService to utilize native vector operations
    // and avoid Hibernate type mapping complexities with pgvector.
    // Column: feature_vector vector(512)
    @jakarta.persistence.Transient
    private float[] dbEmbedding; 
}
