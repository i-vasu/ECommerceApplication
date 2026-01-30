package com.app.catalog.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import com.app.config.AppConstants;
import com.app.catalog.entities.Category;
import com.app.catalog.payloads.CategoryDTO;
import com.app.catalog.payloads.CategoryResponse;
import com.app.catalog.domain.CategoryService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
public class CategoryController implements CategoryApi {

	@Autowired
	private CategoryService categoryService;

	@PostMapping("/admin/category")
	@Override
	public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@Valid @RequestBody Category category) {
		CategoryDTO savedCategoryDTO = categoryService.createCategory(category);

		return new ResponseEntity<>(ApiResponse.success(savedCategoryDTO, "Category created successfully"),
				HttpStatus.CREATED);
	}

	@GetMapping("/public/categories")
	@Override
	public ResponseEntity<ApiResponse<CategoryResponse>> getCategories(
			@RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
			@RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
			@RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORIES_BY, required = false) String sortBy,
			@RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {

		CategoryResponse categoryResponse = categoryService.getCategories(pageNumber, pageSize, sortBy, sortOrder);

		return ResponseEntity.ok(ApiResponse.success(categoryResponse, "Categories retrieved successfully"));
	}

	@PutMapping("/admin/categories/{categoryId}")
	@Override
	public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(@RequestBody Category category,
			@PathVariable Long categoryId) {
		CategoryDTO categoryDTO = categoryService.updateCategory(category, categoryId);

		return ResponseEntity.ok(ApiResponse.success(categoryDTO, "Category updated successfully"));
	}

	@DeleteMapping("/admin/categories/{categoryId}")
	@Override
	public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long categoryId) {
		String status = categoryService.deleteCategory(categoryId);

		return ResponseEntity.ok(ApiResponse.success(status, "Category deleted successfully"));
	}

}
