package com.app.catalog.domain;

import com.app.catalog.entities.Category;
import com.app.catalog.payloads.CategoryDTO;
import com.app.catalog.payloads.CategoryResponse;
import com.app.core.constants.AppConstants;
import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
public class CategoryController implements CategoryApi {

	@Autowired
	private CategoryService categoryService;

	@PostMapping("/admin/category")
	@Override
	public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
		CategoryDTO savedCategoryDTO = categoryService.createCategory(categoryDTO);

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
	public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(@RequestBody CategoryDTO categoryDTO,
			@PathVariable Long categoryId) {
		CategoryDTO updatedCategoryDTO = categoryService.updateCategory(categoryDTO, categoryId);

		return ResponseEntity.ok(ApiResponse.success(updatedCategoryDTO, "Category updated successfully"));
	}

	@DeleteMapping("/admin/categories/{categoryId}")
	@Override
	public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long categoryId) {
		String status = categoryService.deleteCategory(categoryId);

		return ResponseEntity.ok(ApiResponse.success(status, "Category deleted successfully"));
	}

}
