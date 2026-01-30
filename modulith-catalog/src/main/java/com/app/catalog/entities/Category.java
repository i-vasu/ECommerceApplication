package com.app.catalog.entities;

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

import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class Category {

	public Category() {
	}

	public Category(Long categoryId, String categoryName, List<Product> products, SizeChart sizeChart) {
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.products = products;
		this.sizeChart = sizeChart;
	}

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

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}

	public SizeChart getSizeChart() {
		return sizeChart;
	}

	public void setSizeChart(SizeChart sizeChart) {
		this.sizeChart = sizeChart;
	}
}
