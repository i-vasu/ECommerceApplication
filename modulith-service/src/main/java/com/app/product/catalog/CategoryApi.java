package com.app.product.catalog;

import com.app.product.config.AppConstants;
import com.app.product.entites.Category;
import com.app.product.payloads.CategoryDTO;
import com.app.product.payloads.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Category", description = "Category Management APIs")
public interface CategoryApi {

    @Operation(summary = "Create Category", description = "Creates a new product category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully", content = @Content(schema = @Schema(implementation = CategoryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid category details")
    })
    ResponseEntity<CategoryDTO> createCategory(Category category);

    @Operation(summary = "Get All Categories", description = "Retrieves a paginated list of all categories")
    ResponseEntity<CategoryResponse> getCategories(Integer pageNumber, Integer pageSize, String sortBy,
            String sortOrder);

    @Operation(summary = "Update Category", description = "Updates an existing category")
    ResponseEntity<CategoryDTO> updateCategory(Category category, Long categoryId);

    @Operation(summary = "Delete Category", description = "Deletes a category by ID")
    ResponseEntity<String> deleteCategory(Long categoryId);
}
