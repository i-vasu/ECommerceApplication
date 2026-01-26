package com.app.product.entities;

import java.util.List;

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

import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;

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
	private List<Product> products;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "size_chart_id")
	private SizeChart sizeChart;
}
