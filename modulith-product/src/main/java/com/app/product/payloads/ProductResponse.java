package com.app.product.payloads;

import java.util.List;

public record ProductResponse(
		List<ProductDTO> content,
		Integer pageNumber,
		Integer pageSize,
		Long totalElements,
		Integer totalPages,
		boolean lastPage) {
}
