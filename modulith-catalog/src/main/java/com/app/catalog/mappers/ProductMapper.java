package com.app.catalog.mappers;

import com.app.catalog.entities.Product;
import com.app.catalog.payloads.ProductDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "averageRating", source = "reviews", qualifiedByName = "calculateAverageRating")
    @Mapping(target = "sizeGuide", source = "category.sizeChart.data")
    ProductDTO productToProductDTO(Product product);

    Product productDTOToProduct(ProductDTO productDTO);

    @Named("calculateAverageRating")
    default Double calculateAverageRating(List<?> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }

        double sum = reviews.stream()
                .filter(r -> r instanceof com.app.catalog.review.entities.ProductReview)
                .mapToDouble(r -> ((com.app.catalog.review.entities.ProductReview) r).getRating())
                .sum();

        return reviews.isEmpty() ? null : sum / reviews.size();
    }
}
