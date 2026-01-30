package com.app.catalog.services;

import com.app.core.ResourceNotFoundException;
import com.app.security.repositories.UserRepo;
import com.app.catalog.entities.Wishlist;
import com.app.catalog.mappers.ProductMapper;
import com.app.catalog.payloads.ProductDTO;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.repositories.WishlistRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepo wishlistRepo;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getWishlist(String email) {
        Wishlist wishlist = wishlistRepo.findByUserEmail(email)
                .orElseGet(() -> createEmptyWishlist(email));

        return wishlist.getProducts().stream()
                .map(productMapper::productToProductDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void addProductToWishlist(String email, Long productId) {
        Wishlist wishlist = wishlistRepo.findByUserEmail(email)
                .orElseGet(() -> createEmptyWishlist(email));

        var product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (!wishlist.getProducts().contains(product)) {
            wishlist.getProducts().add(product);
            wishlistRepo.save(wishlist);
        }
    }

    @Override
    public void removeProductFromWishlist(String email, Long productId) {
        Wishlist wishlist = wishlistRepo.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist", "email", email));

        var product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        wishlist.getProducts().remove(product);
        wishlistRepo.save(wishlist);
    }

    @Override
    public void clearWishlist(String email) {
        Wishlist wishlist = wishlistRepo.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist", "email", email));

        wishlist.getProducts().clear();
        wishlistRepo.save(wishlist);
    }

    private Wishlist createEmptyWishlist(String email) {
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        return wishlistRepo.save(wishlist);
    }
}
