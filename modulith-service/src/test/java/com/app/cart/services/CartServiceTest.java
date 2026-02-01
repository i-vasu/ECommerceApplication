package com.app.cart;

import com.app.cart.domain.CartServiceImpl;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.repositories.CartItemRepo;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.entities.Product;
import com.app.catalog.repositories.ProductRepo;
import com.app.core.APIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        com.app.security.entities.User user = new com.app.security.entities.User();
        user.setUserId(200L);
        user.setEmail(userEmail);
        testCart.setUserId(user.getUserId());
        testCart.setCartItems(new ArrayList<>());
        testCart.setTotalPrice(BigDecimal.ZERO);

        testProduct = new Product();
        testProduct.setProductId(100L);
        testProduct.setProductName("Test Item");
        testProduct.setPrice(BigDecimal.valueOf(10.0));
        testProduct.setQuantity(50);
    }

    @Test
    void testAddProductToCart_NewItem() {
        when(cartRepo.findById(1L)).thenReturn(Optional.of(testCart));
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        cartService.addProductToCart(1L, 100L, "ITEM001", 2);
        
        assertEquals(1, testCart.getCartItems().size());
        assertEquals(0, BigDecimal.valueOf(20.0).compareTo(testCart.getTotalPrice()));
        verify(cartRepo).save(testCart);
    }

    @Test
    void testAddProductToCart_ExistingItem() {
        CartItem existing = new CartItem();
        existing.setProductId(100L);
        existing.setQuantity(1);
        existing.setProductPrice(BigDecimal.valueOf(10.0));
        testCart.getCartItems().add(existing);
        testCart.setTotalPrice(BigDecimal.valueOf(10.0));

        when(cartRepo.findById(1L)).thenReturn(Optional.of(testCart));
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        cartService.addProductToCart(1L, 100L, "ITEM001", 2);
        
        assertEquals(1, testCart.getCartItems().size());
        assertEquals(3, existing.getQuantity());
        assertEquals(0, BigDecimal.valueOf(30.0).compareTo(testCart.getTotalPrice()));
    }

    @Test
    void testAddProductToCart_InsufficientStock() {
        testProduct.setQuantity(1);
        when(cartRepo.findById(1L)).thenReturn(Optional.of(testCart));
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        assertThrows(APIException.class, () -> 
            cartService.addProductToCart(1L, 100L, "ITEM001", 5)
        );
    }

    @Test
    void testUpdateProductQuantity() {
        CartItem existing = new CartItem();
        existing.setProductId(100L);
        existing.setQuantity(1);
        existing.setProductPrice(BigDecimal.valueOf(10.0));
        testCart.getCartItems().add(existing);
        testCart.setTotalPrice(BigDecimal.valueOf(10.0));

        when(cartRepo.findById(1L)).thenReturn(Optional.of(testCart));
        when(productRepo.findById(100L)).thenReturn(Optional.of(testProduct));
        
        cartService.updateProductQuantityInCart(1L, 100L, "ITEM001", 5);
        
        assertEquals(5, existing.getQuantity());
        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(testCart.getTotalPrice()));
    }

    @Test
    void testDeleteProductFromCart() {
        CartItem existing = new CartItem();
        existing.setProductId(100L);
        existing.setQuantity(1);
        existing.setProductPrice(BigDecimal.valueOf(10.0));
        testCart.getCartItems().add(existing);
        testCart.setTotalPrice(BigDecimal.valueOf(10.0));

        when(cartRepo.findById(1L)).thenReturn(Optional.of(testCart));
        
        cartService.deleteProductFromCart(1L, 100L);
        
        assertEquals(0, testCart.getCartItems().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(testCart.getTotalPrice()));
    }
}
