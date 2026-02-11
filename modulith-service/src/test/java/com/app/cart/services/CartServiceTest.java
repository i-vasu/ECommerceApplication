package com.app.cart.services;

import com.app.cart.domain.CartServiceImpl;
import com.app.cart.domain.services.CartCouponService;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.repositories.CartItemRepo;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.ProductService;
import com.app.catalog.payloads.ProductDTO;
import com.app.core.APIException;
import com.app.core.services.RedisLockService;
import com.app.finance.pricing.OrderTotalService;
import com.app.finance.pricing.contracts.OrderSummary;
import com.app.governance.rules.RuleEngineService;
import com.app.intelligence.analysis.services.AnalyticsService;
import com.app.logistics.inventory.InventoryReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepo cartRepo;
    @Mock
    private CartItemRepo cartItemRepo;
    @Mock
    private ProductService productService;
    @Mock
    private RedisLockService lockService;
    @Mock
    private OrderTotalService orderTotalService;
    @Mock
    private InventoryReservationService inventoryReservationService;
    @Mock
    private CartCouponService cartCouponService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private RuleEngineService ruleEngine;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart testCart;
    private ProductDTO testProduct;
    private Long cartId = 1L;
    private Long productId = 100L;
    private String itemCode = "ITEM001";

    @BeforeEach
    void setUp() {
        testCart = new Cart();
        testCart.setCartId(cartId);
        testCart.setUserId(200L);
        testCart.setCartItems(new ArrayList<>());
        testCart.setTotalPrice(BigDecimal.ZERO);

        testProduct = new ProductDTO(
            productId, 
            "Test Product", 
            itemCode, 
            "img.png", 
            "Test Description", 
            50, 
            BigDecimal.valueOf(100.0), 
            BigDecimal.valueOf(10.0), 
            BigDecimal.valueOf(90.0), 
            List.of(), 
            List.of(), 
            List.of(), 
            4.5
        );
    }

    @Test
    void testAddProductToCart_NewItem() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(testCart));
        when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);
        when(productService.getProductById(productId)).thenReturn(testProduct);
        when(cartItemRepo.findCartItemByProductIdAndCartIdAndItemCode(eq(cartId), eq(productId), anyString())).thenReturn(null);
        when(inventoryReservationService.checkStock(anyString(), anyInt())).thenReturn(true);
        
        OrderSummary orderSummary = new OrderSummary();
        orderSummary.setFinalTotal(BigDecimal.valueOf(180.0));
        when(orderTotalService.calculate(any())).thenReturn(orderSummary);
        
        // Act
        cartService.addProductToCart(cartId, productId, itemCode, 2);
        
        // Assert
        verify(cartItemRepo).save(any(CartItem.class));
        verify(analyticsService).trackAddToCart(anyLong(), anyString(), any());
        verify(lockService).unlock(anyString());
    }

    @Test
    void testAddProductToCart_ExistingItem_ShouldThrowException() {
        // Arrange
        CartItem existingItem = new CartItem();
        existingItem.setProductId(productId);
        existingItem.setItemCode(itemCode);
        existingItem.setQuantity(1);
        existingItem.setProductPrice(BigDecimal.valueOf(90.0));
        
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(testCart));
        when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);
        when(productService.getProductById(productId)).thenReturn(testProduct);
        when(cartItemRepo.findCartItemByProductIdAndCartIdAndItemCode(eq(cartId), eq(productId), anyString())).thenReturn(existingItem);
        
        // Act & Assert - Should throw exception because item already exists
        assertThrows(APIException.class, () -> 
            cartService.addProductToCart(cartId, productId, itemCode, 2)
        );
        
        verify(lockService).unlock(anyString());
    }

    @Test
    void testAddProductToCart_InsufficientStock() {
        // Arrange
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(testCart));
        when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);
        when(productService.getProductById(productId)).thenReturn(testProduct);
        when(cartItemRepo.findCartItemByProductIdAndCartIdAndItemCode(eq(cartId), eq(productId), anyString())).thenReturn(null);
        when(inventoryReservationService.checkStock(anyString(), anyInt())).thenReturn(false);
        
        // Act & Assert
        assertThrows(APIException.class, () -> 
            cartService.addProductToCart(cartId, productId, itemCode, 100)
        );
        
        verify(lockService).unlock(anyString());
    }

    @Test
    void testUpdateProductQuantity() {
        // Arrange
        CartItem existingItem = new CartItem();
        existingItem.setProductId(productId);
        existingItem.setItemCode(itemCode);
        existingItem.setQuantity(1);
        existingItem.setProductPrice(BigDecimal.valueOf(90.0));
        testCart.getCartItems().add(existingItem);
        
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(testCart));
        when(cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId)).thenReturn(existingItem);
        when(inventoryReservationService.checkStock(anyString(), anyInt())).thenReturn(true);
        
        OrderSummary orderSummary = new OrderSummary();
        orderSummary.setFinalTotal(BigDecimal.valueOf(450.0));
        when(orderTotalService.calculate(any())).thenReturn(orderSummary);
        
        // Act
        cartService.updateProductQuantityInCart(cartId, productId, itemCode, 5);
        
        // Assert
        assertEquals(5, existingItem.getQuantity());
    }

    @Test
    void testDeleteProductFromCart() {
        // Arrange
        CartItem existingItem = new CartItem();
        existingItem.setProductId(productId);
        existingItem.setItemCode(itemCode);
        existingItem.setQuantity(1);
        existingItem.setProductPrice(BigDecimal.valueOf(90.0));
        testCart.getCartItems().add(existingItem);
        
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(testCart));
        when(cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId)).thenReturn(existingItem);
        
        OrderSummary orderSummary = new OrderSummary();
        orderSummary.setFinalTotal(BigDecimal.ZERO);
        when(orderTotalService.calculate(any())).thenReturn(orderSummary);
        
        // Act
        cartService.deleteProductFromCart(cartId, productId);
        
        // Assert
        verify(cartItemRepo).delete(existingItem);
        assertEquals(0, testCart.getCartItems().size());
    }
}
