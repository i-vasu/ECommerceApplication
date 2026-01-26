package com.app.search.services;

import com.app.product.payloads.ProductDTO;
import java.util.List;

public interface SearchService {
    void indexProduct(ProductDTO product);

    void indexProducts(List<ProductDTO> products);

    List<ProductDTO> searchProducts(String query, Double minPrice, Double maxPrice);

    void deleteProduct(String productId);
}
