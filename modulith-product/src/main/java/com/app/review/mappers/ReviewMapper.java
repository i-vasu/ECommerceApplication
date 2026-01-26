package com.app.review.mappers;

import com.app.review.entities.ProductReview;
import com.app.review.payloads.ProductReviewDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ProductReviewDTO toDTO(ProductReview review);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ProductReview toEntity(ProductReviewDTO reviewDTO);
}
