package com.app.product.catalog;

import com.app.product.entities.Category;
import com.app.product.payloads.CategoryDTO;
import com.app.product.payloads.CategoryResponse;
import com.app.core.payloads.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Category v1", description = "Category Management APIs - Version 1")
public interface CategoryApi {

        @Operation(summary = "Create Category", description = "Creates a new product category")
        @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully", content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid category details")
        })
        ResponseEntity<ApiResponse<CategoryDTO>> createCategory(Category category);

        @Operation(summary = "Get All Categories", description = "Retrieves a paginated list of all categories")
        ResponseEntity<ApiResponse<CategoryResponse>> getCategories(Integer pageNumber, Integer pageSize, String sortBy,
                        String sortOrder);

        @Operation(summary = "Update Category", description = "Updates an existing category")
        ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(Category category, Long categoryId);

        @Operation(summary = "Delete Category", description = "Deletes a category by ID")
        ResponseEntity<ApiResponse<String>> deleteCategory(Long categoryId);
}
