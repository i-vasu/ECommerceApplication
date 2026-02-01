package com.app.catalog.payloads;

import java.util.List;

public record CategoryResponse(
        List<CategoryDTO> content,
        Integer pageNumber,
        Integer pageSize,
        Long totalElements,
        Integer totalPages,
        boolean lastPage) {
}
