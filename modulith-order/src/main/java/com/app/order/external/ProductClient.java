package com.app.order.external;

import java.util.List;
import com.app.catalog.payloads.ProductDTO;

public interface ProductClient {

    ProductDTO getProduct(Long productId);

    List<ProductDTO> getProductsBatch(List<Long> productIds);
}
