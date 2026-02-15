package com.app.catalog.domain;

import com.app.catalog.ProductService;
import com.app.catalog.entities.Product;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.payloads.ProductResponse;
import com.app.core.constants.AppConstants;
import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@SecurityRequirement(name = "E-Commerce Application")
public class ProductController implements ProductApi {

    @Autowired
    private ProductService productService;

    @PostMapping("/admin/categories/{categoryId}/product")
    @Override
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@Valid @RequestBody ProductDTO productDTO,
            @PathVariable Long categoryId) {
        ProductDTO savedProductDTO = productService.addProduct(categoryId, productDTO);
        return new ResponseEntity<>(ApiResponse.success(savedProductDTO, "Product created successfully"),
                HttpStatus.CREATED);
    }

    @GetMapping("/public/products")
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> getProducts(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        ProductResponse productResponse = productService.getAllProducts(pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(productResponse, "Products retrieved successfully"));
    }

    @GetMapping("/public/categories/{categoryId}/products")
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        ProductResponse productResponse = productService.searchByCategory(categoryId, pageNumber, pageSize, sortBy,
                sortOrder);
        return ResponseEntity.ok(ApiResponse.success(productResponse, "Products retrieved successfully"));
    }

    @GetMapping("/public/products/keyword/{keyword}")
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> getProductsByKeyword(@PathVariable String keyword,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        if (keyword == null || keyword.trim().length() < 3) {
            throw new com.app.core.APIException("Search keyword must be at least 3 characters long.");
        }
        ProductResponse productResponse = productService.searchProductByKeyword(keyword, pageNumber, pageSize, sortBy,
                sortOrder);
        return ResponseEntity.ok(ApiResponse.success(productResponse, "Products retrieved successfully"));
    }

    @GetMapping("/public/products/search")
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> facetedSearch(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCTS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder) {
        ProductResponse productResponse = productService.facetedSearch(keyword, minPrice, maxPrice, pageNumber,
                pageSize,
                sortBy, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(productResponse, "Products retrieved successfully"));
    }

    @GetMapping("/public/products/{productId}")
    @Override
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable String productId) {
        // Storefront might send handle/itemCode as productId.
        try {
            Long id = Long.parseLong(productId);
            ProductDTO productDTO = productService.getProductById(id);
            return ResponseEntity.ok(ApiResponse.success(productDTO, "Product retrieved successfully"));
        } catch (NumberFormatException e) {
            // Fallback: search by itemCode (slug)
            ProductDTO productDTO = productService.getProductByCode(productId);
            return ResponseEntity.ok(ApiResponse.success(productDTO, "Product retrieved successfully"));
        }
    }

    @PutMapping("/admin/products/{productId}")
    @Override
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(@Valid @RequestBody ProductDTO productDTO,
            @PathVariable Long productId) {
        ProductDTO updatedProductDTO = productService.updateProduct(productId, productDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedProductDTO, "Product updated successfully"));
    }

    @DeleteMapping("/admin/products/{productId}")
    @Override
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long productId) {
        String status = productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(status, "Product deleted successfully"));
    }
}
