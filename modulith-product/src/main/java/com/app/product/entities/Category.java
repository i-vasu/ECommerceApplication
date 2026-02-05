package com.app.product.entities;

import java.util.List;

import org.hibernate.annotations.BatchSize;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long categoryId;

	@NotBlank
	@Size(min = 5, message = "Category name must contain atleast 5 characters")
	private String categoryName;

	@OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
	// Bolt: Batch fetching to solve N+1 query problem
	@BatchSize(size = 20)
	private List<Product> products;
}
