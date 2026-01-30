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

import com.app.erp_sync.gateway.ERPNextService;
import com.app.catalog.repositories.ProductRepo;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.security.repositories.AddressRepo;
import com.app.order.repositories.OrderHistoryRepo;
import com.app.order.async.OrderProducer;
import com.app.core.async.EventProducer;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.rules.RuleEngineService;
import com.app.core.services.RedisLockService;
import com.app.security.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import com.app.cart.domain.CartService;
import com.app.core.APIException;
import com.app.security.repositories.UserRepo;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.finance.entities.Payment;
import com.app.order.mappers.OrderMapper;
import com.app.cart.repositories.CartRepo;
import com.app.order.repositories.OrderRepo;
import com.app.finance.repositories.PaymentRepo;
import com.app.order.repositories.OrderItemRepo;

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
    @Mock
    private OptimizedCheckoutService optimizedCheckoutService;
    @Mock
    private AddressRepo addressRepo;
    @Mock
    private OrderHistoryRepo orderHistoryRepo;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Tracer tracer;
    @Mock
    private OrderProducer orderProducer;
    @Mock
    private EventProducer eventProducer;
    @Mock
    private OperationalStateMachineService operationalStateMachine;
    @Mock
    private RuleEngineService ruleEngine;
    @Mock
    private RedisLockService lockService;
    @Mock
    private UserService userService;

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
        
        // Mock optimized checkout result
        OptimizedCheckoutService.CheckoutResult result = mock(OptimizedCheckoutService.CheckoutResult.class);
        when(result.inventory()).thenReturn(new OptimizedCheckoutService.CheckoutResult.InventoryStatus(true, "OK"));
        when(optimizedCheckoutService.processCheckout(any(), any())).thenReturn(result);

        // Mock lock
        when(lockService.tryLock(anyString(), any())).thenReturn(true);
        
        // Mock Span/Tracer
        io.micrometer.tracing.Span span = mock(io.micrometer.tracing.Span.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);

        // DB Failure simulation on order save
        when(orderRepo.save(any())).thenThrow(new RuntimeException("DB Error"));

        // Act & Assert
        assertThrows(APIException.class, () -> orderService.placeOrder(email, cartId, payMethod));

        // Assert Stock Release
        verify(inventoryReservationService).releaseStock("ITEM-1", 2);
    }
}
