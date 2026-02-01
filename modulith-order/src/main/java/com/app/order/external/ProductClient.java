package com.app.order.external;

import com.app.catalog.payloads.ProductDTO;

import java.util.List;

public interface ProductClient {

    ProductDTO getProduct(Long productId);

    List<ProductDTO> getProductsBatch(List<Long> productIds);
}
