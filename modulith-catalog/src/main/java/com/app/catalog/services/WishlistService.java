package com.app.catalog.services;

import com.app.catalog.payloads.ProductDTO;

import java.util.List;

public interface WishlistService {
    List<ProductDTO> getWishlist(String email);

    void addProductToWishlist(String email, Long productId);

    void removeProductFromWishlist(String email, Long productId);

    void clearWishlist(String email);
}
