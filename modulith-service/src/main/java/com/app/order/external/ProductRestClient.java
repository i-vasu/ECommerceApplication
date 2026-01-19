package com.app.order.external;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

import com.app.product.payloads.ProductDTO;

@Component
public class ProductRestClient implements ProductClient {

    private final RestClient restClient;
    private static final ParameterizedTypeReference<List<ProductDTO>> LIST_PRODUCT_TYPE = new ParameterizedTypeReference<>() {
    };

    public ProductRestClient(@Value("${product.service.url:http://localhost:8080}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public ProductDTO getProduct(Long productId) {
        return restClient.get()
                .uri("/api/public/products/{productId}", productId)
                .retrieve()
                .body(ProductDTO.class);
    }

    @Override
    public List<ProductDTO> getProductsBatch(List<Long> productIds) {
        return restClient.post()
                .uri("/api/public/products/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(productIds)
                .retrieve()
                .body(LIST_PRODUCT_TYPE);
    }
}
