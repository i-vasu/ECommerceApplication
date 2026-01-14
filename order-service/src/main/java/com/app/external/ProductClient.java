package com.app.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.app.payloads.ProductDTO;

@FeignClient(name = "product-service", url = "${product.service.url:http://localhost:8082}")
public interface ProductClient {

    @GetMapping("/api/public/products/{productId}")
    ProductDTO getProduct(@PathVariable("productId") Long productId);

    @org.springframework.web.bind.annotation.PostMapping("/api/public/products/batch")
    java.util.List<ProductDTO> getProductsBatch(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<Long> productIds);

    // Add inventory check/reserved logic later
}
