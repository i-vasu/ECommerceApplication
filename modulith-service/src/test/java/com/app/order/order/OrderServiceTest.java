package com.app.order.order;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.app.cart.CartService;
import com.app.core.APIException;
import com.app.identity.repositories.UserRepo;
import com.app.inventory.InventoryReservationService;
import com.app.order.entities.Cart;
import com.app.order.entities.CartItem;
import com.app.order.entities.Payment;
import com.app.order.mappers.OrderMapper;
import com.app.order.repositories.CartRepo;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.PaymentRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.services.ERPNextService;
import com.app.product.repositories.ProductRepo;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CartRepo cartRepo;
    @Mock
    private OrderRepo orderRepo;
    @Mock
    private PaymentRepo paymentRepo;
    @Mock
    private OrderItemRepo orderItemRepo;
    @Mock
    private CartService cartService;
    @Mock
    private ERPNextService erpNextService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private InventoryReservationService inventoryReservationService;
    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void placeOrder_shouldReleaseStock_whenOrderCreationFails() {
        // Arrange
        String email = "test@test.com";
        Long cartId = 1L;
        String payMethod = "COD";

        Cart cart = new Cart();
        cart.setCartId(cartId);
        cart.setTotalPrice(100.0);
        CartItem item = new CartItem();
        item.setItemCode("ITEM-1");
        item.setQuantity(2);
        item.setProductId(10L);
        item.setProductPrice(50.0);
        cart.setCartItems(Collections.singletonList(item));

        when(cartRepo.findCartByEmailAndCartId(email, cartId)).thenReturn(cart);
        // Reserve success
        when(inventoryReservationService.reserveStock("ITEM-1", 2)).thenReturn(true);
        // DB Failure simulation (e.g. Payment save fails)
        when(paymentRepo.save(any(Payment.class))).thenThrow(new RuntimeException("DB Error"));

        // Act & Assert
        // Expect APIException which wraps the runtime exception
        assertThrows(APIException.class, () -> orderService.placeOrder(email, cartId, payMethod));

        // Assert Stock Release
        verify(inventoryReservationService).releaseStock("ITEM-1", 2);
    }
}
