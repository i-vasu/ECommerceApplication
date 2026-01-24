package com.app.review.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.app.product.entities.Product;

@Entity
@Table(name = "product_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    private Long userId;
    private String userName;
    private String email;
    private int rating;
    private String comment;
    private boolean isVerifiedPurchase = false;
    private boolean approved = true; // Auto-approve for now, can be toggled by admin
    private int helpfulCount = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
}
