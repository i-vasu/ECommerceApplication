package com.app.product.mappers;

import com.app.product.entities.Product;
import com.app.product.payloads.ProductDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "averageRating", source = "reviews", qualifiedByName = "calculateAverageRating")
    ProductDTO productToProductDTO(Product product);

    Product productDTOToProduct(ProductDTO productDTO);

    @Named("calculateAverageRating")
    default Double calculateAverageRating(List<?> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }

        double sum = reviews.stream()
                .filter(r -> r instanceof com.app.review.entities.ProductReview)
                .mapToDouble(r -> ((com.app.review.entities.ProductReview) r).getRating())
                .sum();

        return reviews.isEmpty() ? null : sum / reviews.size();
    }
}
