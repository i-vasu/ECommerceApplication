package com.app.cart;

import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.repositories.CartRepo;
import com.app.cart.repositories.CartItemRepo;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepo cartRepo;
    @Mock
    private CartItemRepo cartItemRepo;
    @Mock
    private ProductRepo productRepo;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart testCart;
    private Product testProduct;
    private String userEmail = "cart@example.com";

    @BeforeEach
    void setUp() {
        testCart = new Cart();
        testCart.setCartId(1L);
        testCart.setEmail(userEmail);
        testCart.setCartItems(new ArrayList<>());
        testCart.setTotalPrice(0.0);

        testProduct = new Product();
        testProduct.setProductId(100L);
        testProduct.setProductName("Test Item");
        testProduct.setPrice(BigDecimal.valueOf(10.0));
        testProduct.setQuantity(50);
    }

    @Test
    void testAddProductToCart_NewItem() {
        when(cartRepo.findCartByEmailAndCartId(userEmail, 1L)).thenReturn(testCart);
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        cartService.addProductToCart(1L, 100L, 2, userEmail);
        
        assertEquals(1, testCart.getCartItems().size());
        assertEquals(20.0, testCart.getTotalPrice());
        verify(cartRepo).save(testCart);
    }

    @Test
    void testAddProductToCart_ExistingItem() {
        CartItem existing = new CartItem();
        existing.setProductId(100L);
        existing.setQuantity(1);
        existing.setProductPrice(10.0);
        testCart.getCartItems().add(existing);
        testCart.setTotalPrice(10.0);

        when(cartRepo.findCartByEmailAndCartId(userEmail, 1L)).thenReturn(testCart);
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        cartService.addProductToCart(1L, 100L, 2, userEmail);
        
        assertEquals(1, testCart.getCartItems().size());
        assertEquals(3, existing.getQuantity());
        assertEquals(30.0, testCart.getTotalPrice());
    }

    @Test
    void testAddProductToCart_InsufficientStock() {
        testProduct.setQuantity(1);
        when(cartRepo.findCartByEmailAndCartId(userEmail, 1L)).thenReturn(testCart);
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        assertThrows(APIException.class, () -> 
            cartService.addProductToCart(1L, 100L, 5, userEmail)
        );
    }

    @Test
    void testUpdateProductQuantity() {
        CartItem existing = new CartItem();
        existing.setProductId(100L);
        existing.setQuantity(1);
        existing.setProductPrice(10.0);
        testCart.getCartItems().add(existing);
        testCart.setTotalPrice(10.0);

        when(cartRepo.findCartByEmailAndCartId(userEmail, 1L)).thenReturn(testCart);
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        cartService.updateProductQuantity(1L, 100L, 5, userEmail);
        
        assertEquals(5, existing.getQuantity());
        assertEquals(50.0, testCart.getTotalPrice());
    }

    @Test
    void testDeleteProductFromCart() {
        CartItem existing = new CartItem();
        existing.setProductId(100L);
        existing.setQuantity(1);
        existing.setProductPrice(10.0);
        testCart.getCartItems().add(existing);
        testCart.setTotalPrice(10.0);

        when(cartRepo.findCartByEmailAndCartId(userEmail, 1L)).thenReturn(testCart);
        
        cartService.deleteProductFromCart(1L, 100L);
        
        assertEquals(0, testCart.getCartItems().size());
        assertEquals(0.0, testCart.getTotalPrice());
    }
}
