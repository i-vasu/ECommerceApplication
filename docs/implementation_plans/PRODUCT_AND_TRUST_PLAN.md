---
name: Product & Trust Phase Implementation Plan
description: Plan to implement Product Enhancements and Trust (Reviews/Ratings) features.
---

# Feature: Product & Trust (Reviews & Ratings)

## Objective
Implement a robust Review and Rating system for products to build trust. This includes CRUD operations for reviews, calculating average product ratings, and ensuring only verified purchases can leave verified reviews (optional but recommended).

## Status
- [x] Product Review Entity
- [x] Review Repository
- [x] Review DTOs (Refinement)
- [x] Review Service (Implementation)
- [x] Review Controller (API)
- [x] Update Product Service to include Average Rating / Review Summary
- [ ] Integration Tests
- [ ] Fix Legacy Code (UserServiceImpl uses setters on records - needs refactoring)

## Detailed Steps

### 1. Repository Layer
- Create `ProductReviewRepo` in `com.app.review.repositories`.
- Methods: `findAllByProduct`, `findByUserIdAndProduct`.

### 2. Service Layer
- Create `ReviewService` interface and `ReviewServiceImpl`.
- Implement `addReview(Long productId, ProductReviewDTO reviewDTO)`.
- Implement `getReviewsByProduct(Long productId)`.
- Implement `deleteReview(Long reviewId)`.
- Logic to update Product's average rating when a review is added/deleted (optional: event driven or direct update).

### 3. Controller Layer
- Create `ReviewController`.
- Endpoints:
    - `POST /api/v1/products/{productId}/reviews`
    - `GET /api/v1/products/{productId}/reviews`
    - `DELETE /api/v1/reviews/{reviewId}`

### 4. Integration with Product
- Ensure `ProductDTO` includes `averageRating` and `numberOfReviews` (or list of reviews).
- Update `ProductServiceImpl` to potentially fetch these details.

### 5. DTOs
- Ensure `ProductReviewDTO` is well defined in `com.app.review.payloads`.
