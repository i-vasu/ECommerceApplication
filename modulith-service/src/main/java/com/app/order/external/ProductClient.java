package com.app.order.external;

import java.util.List;
import com.app.product.payloads.ProductDTO;

public interface ProductClient {

    ProductDTO getProduct(Long productId);

    List<ProductDTO> getProductsBatch(List<Long> productIds);
}
