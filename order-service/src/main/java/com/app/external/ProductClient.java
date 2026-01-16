package com.app.external;

import java.util.List;
import com.app.payloads.ProductDTO;

public interface ProductClient {

    ProductDTO getProduct(Long productId);

    List<ProductDTO> getProductsBatch(List<Long> productIds);
}
