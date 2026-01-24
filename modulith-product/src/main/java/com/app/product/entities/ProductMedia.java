package com.app.product.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    // IMAGE, VIDEO
    private String type;

    // URL
    private String url;

    // Sort order in the gallery
    private int displayOrder;

    private String blurHash;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
