package com.app.review.services;

import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.review.entities.ProductReview;
import com.app.catalog.review.mappers.ReviewMapper;
import com.app.catalog.review.payloads.ProductReviewDTO;
import com.app.catalog.review.repositories.ProductReviewRepo;
import com.app.catalog.review.services.ReviewServiceImpl;
import com.app.core.APIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ProductReviewRepo reviewRepo;

    @Mock
    private ProductRepo productRepo;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void testAddReview_Success() {
        Long productId = 1L;
        ProductReviewDTO reviewDTO = new ProductReviewDTO(null, 1L, "user1", "",5, "Great!", true, null);

        Product product = new Product();
        product.setProductId(productId);

        ProductReview review = new ProductReview();
        review.setRating(5);
        review.setComment("Great!");

        when(productRepo.findById(productId)).thenReturn(Optional.of(product));
        when(reviewMapper.toEntity(any(ProductReviewDTO.class))).thenReturn(review);
        when(reviewRepo.save(any(ProductReview.class))).thenReturn(review);
        when(reviewMapper.toDTO(any(ProductReview.class))).thenReturn(reviewDTO);

        ProductReviewDTO saved = reviewService.addReview(productId, reviewDTO);

        assertNotNull(saved);
        assertEquals(5, saved.rating());
        assertEquals("Great!", saved.comment());
        verify(reviewRepo, times(1)).save(any(ProductReview.class));
    }

    @Test
    void testAddReview_InvalidRating() {
        Long productId = 1L;
        ProductReviewDTO reviewDTO = new ProductReviewDTO(null, 1L, "user1", "",0, "Bad", true, null);

        Product product = new Product();
        product.setProductId(productId);

        ProductReview review = new ProductReview();
        review.setRating(0);

        when(productRepo.findById(productId)).thenReturn(Optional.of(product));
        when(reviewMapper.toEntity(any(ProductReviewDTO.class))).thenReturn(review);

        assertThrows(APIException.class, () -> reviewService.addReview(productId, reviewDTO));
    }

    @Test
    void testGetReviewsByProduct() {
        Long productId = 1L;
        Product product = new Product();
        product.setProductId(productId);
        ProductReview review = new ProductReview();
        review.setRating(4);
        ProductReviewDTO reviewDTO = new ProductReviewDTO(1L, 1L, "user1", "",4, "Good", true, null);

        when(productRepo.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepo.findByProduct(product)).thenReturn(List.of(review));
        when(reviewMapper.toDTO(review)).thenReturn(reviewDTO);

        List<ProductReviewDTO> reviews = reviewService.getReviewsByProduct(productId);

        assertEquals(1, reviews.size());
        assertEquals(4, reviews.get(0).rating());
    }

    @Test
    void testGetAverageRating() {
        Long productId = 1L;
        when(reviewRepo.getAverageRatingByProductId(productId)).thenReturn(4.5);

        Double avg = reviewService.getAverageRating(productId);

        assertEquals(4.5, avg);
    }
}
