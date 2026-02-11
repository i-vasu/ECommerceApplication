package com.app.cart.domain;

import com.app.cart.domain.services.CartCouponService;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.payloads.CartDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class CartServiceImplTest {

    @Mock
    private CartRepo cartRepo;
    @Mock
    private ProductService productService;
    @Mock
    private CartItemRepo cartItemRepo;
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
    @Mock
    private RedisLockService lockService;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private Long cartId = 1L;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setCartId(cartId);
        cart.setTotalPrice(BigDecimal.ZERO);
        cart.setCartItems(new ArrayList<>());
    }

    @Test
    void testCreateCart() {
        when(cartRepo.save(any(Cart.class))).thenReturn(cart);

        CartDTO result = cartService.createCart();

        assertNotNull(result);
        assertEquals(cartId, result.cartId());
        verify(cartRepo).save(any(Cart.class));
    }

    @Test
    void testAddProductToCart_Success() {
        Long productId = 100L;
        String itemCode = "ITEM001";
        Integer quantity = 2;

        ProductDTO productDTO = new ProductDTO(productId, "Product 1", itemCode, "img.png", "desc", 10, BigDecimal.valueOf(100.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(90.0), List.of(), List.of(), List.of(), 5.0);

        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(cart));
        when(ruleEngine.evaluate(anyString(), anyMap())).thenReturn(true);
        when(productService.getProductById(productId)).thenReturn(productDTO);
        when(cartItemRepo.findCartItemByProductIdAndCartIdAndItemCode(eq(cartId), eq(productId), anyString())).thenReturn(null);
        when(inventoryReservationService.checkStock(anyString(), anyInt())).thenReturn(true);
        
        OrderSummary response = new OrderSummary();
        response.setFinalTotal(BigDecimal.valueOf(180.0));
        when(orderTotalService.calculate(any())).thenReturn(response);

        CartDTO result = cartService.addProductToCart(cartId, productId, itemCode, quantity);

        assertNotNull(result);
        verify(cartItemRepo).save(any(CartItem.class));
        verify(analyticsService).trackAddToCart(anyLong(), anyString(), any());
        verify(lockService).unlock(anyString());
    }

    @Test
    void testAddProductToCart_LockFailed() {
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(false);

        assertThrows(APIException.class, () -> cartService.addProductToCart(cartId, 100L, "CODE", 1));
    }

    @Test
    void testApplyCoupon_Success() {
        String couponCode = "SAVE10";
        cart.getCartItems().add(new CartItem(1L, cart, 100L, "CODE", "Product", 1, BigDecimal.ZERO, BigDecimal.valueOf(100.0)));
        
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(cart));
        
        OrderSummary response = new OrderSummary();
        response.setFinalTotal(BigDecimal.valueOf(90.0));
        when(orderTotalService.calculate(any())).thenReturn(response);

        CartDTO result = cartService.applyCoupon(cartId, couponCode);

        assertNotNull(result);
        assertEquals(couponCode, cart.getCouponCode());
        verify(cartCouponService).applyCouponToCart(eq(couponCode), any(BigDecimal.class));
    }

    @Test
    void testDeleteProductFromCart_Success() {
        Long productId = 100L;
        CartItem item = new CartItem(1L, cart, productId, "CODE", "Product", 1, BigDecimal.ZERO, BigDecimal.valueOf(100.0));
        cart.getCartItems().add(item);

        when(cartRepo.findById(cartId)).thenReturn(Optional.of(cart));
        when(cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId)).thenReturn(item);
        
        OrderSummary response = new OrderSummary();
        response.setFinalTotal(BigDecimal.ZERO);
        when(orderTotalService.calculate(any())).thenReturn(response);

        String result = cartService.deleteProductFromCart(cartId, productId);

        assertEquals("Product removed from the cart", result);
        verify(cartItemRepo).delete(item);
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void testUpdateProductQuantity_Success() {
        Long productId = 100L;
        CartItem item = new CartItem(1L, cart, productId, "CODE", "Product", 1, BigDecimal.ZERO, BigDecimal.valueOf(100.0));
        
        when(cartRepo.findById(cartId)).thenReturn(Optional.of(cart));
        when(cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId)).thenReturn(item);
        when(inventoryReservationService.checkStock(anyString(), anyInt())).thenReturn(true);
        
        OrderSummary response = new OrderSummary();
        response.setFinalTotal(BigDecimal.valueOf(200.0));
        when(orderTotalService.calculate(any())).thenReturn(response);

        CartDTO result = cartService.updateProductQuantityInCart(cartId, productId, "CODE", 2);

        assertNotNull(result);
        assertEquals(2, item.getQuantity());
    }
}
