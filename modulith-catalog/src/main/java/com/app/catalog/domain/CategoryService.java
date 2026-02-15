package com.app.catalog.domain;

import com.app.catalog.entities.Category;
import com.app.catalog.payloads.CategoryDTO;
import com.app.catalog.payloads.CategoryResponse;

public interface CategoryService {

	CategoryDTO createCategory(CategoryDTO categoryDTO);

	CategoryResponse getCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

	CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId);

	String deleteCategory(Long categoryId);
}
