package com.app.review.payloads;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductReviewDTO {

    private Long reviewId;
    private Long userId;
    private String userName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
