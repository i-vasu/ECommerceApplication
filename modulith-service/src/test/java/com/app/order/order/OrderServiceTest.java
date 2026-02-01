package com.app.order.order;

import com.app.cart.domain.CartService;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.core.APIException;
import com.app.core.async.EventProducer;
import com.app.core.services.RedisLockService;
import com.app.erp_sync.gateway.ERPNextService;
import com.app.finance.repositories.PaymentRepo;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.order.async.OrderProducer;
import com.app.order.mappers.OrderMapper;
import com.app.order.repositories.OrderHistoryRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.repositories.OrderRepo;
import com.app.security.UserService;
import com.app.security.repositories.AddressRepo;
import com.app.security.repositories.UserRepo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    private MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
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

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(orderService, "meterRegistry", meterRegistry);
        orderService.initMetrics();

        // Mock Tracer/Span
        io.micrometer.tracing.Span span = mock(io.micrometer.tracing.Span.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.start()).thenReturn(span);
        
        // Mock lock
        when(lockService.tryLock(anyString(), any())).thenReturn(true);
        lenient().when(cartRepo.findCartByUserIdAndCartId(anyLong(), anyLong())).thenReturn(null); // will be stubbed in test
    }

    @Test
    void placeOrder_shouldReleaseStock_whenOrderCreationFails() {
        // Arrange
        String email = "test@test.com";
        Long cartId = 1L;
        String payMethod = "COD";

        Cart cart = new Cart();
        cart.setCartId(cartId);
        com.app.security.entities.User user = new com.app.security.entities.User();
        user.setUserId(100L);
        user.setEmail(email);
        cart.setUserId(user.getUserId());
        cart.setTotalPrice(BigDecimal.valueOf(100.0));
        CartItem item = new CartItem();
        item.setItemCode("ITEM-1");
        item.setQuantity(2);
        item.setProductId(10L);
        item.setProductPrice(BigDecimal.valueOf(50.0));
        cart.setCartItems(Collections.singletonList(item));

        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(cartRepo.findCartByUserIdAndCartId(user.getUserId(), cartId)).thenReturn(cart);
        
        // Mock optimized checkout result
        com.app.logistics.inventory.InventoryService.InventoryLock invLock = mock(com.app.logistics.inventory.InventoryService.InventoryLock.class);
        when(invLock.locked()).thenReturn(true);

        OptimizedCheckoutService.CheckoutResult result = new OptimizedCheckoutService.CheckoutResult(
            invLock,
            mock(com.app.security.AddressValidationService.AddressValidation.class),
            mock(com.app.logistics.shipping.TaxCalculationService.TaxCalculation.class),
            mock(com.app.logistics.shipping.ShippingCalculationService.ShippingCost.class),
            BigDecimal.ZERO,
            BigDecimal.valueOf(100.0),
            new java.util.ArrayList<>()
        );
        when(optimizedCheckoutService.processCheckout(any(), any())).thenReturn(result);

        // DB Failure simulation on order save
        when(orderRepo.save(any())).thenThrow(new RuntimeException("DB Error"));

        // Act & Assert
        assertThrows(APIException.class, () -> orderService.placeOrder(email, cartId, payMethod));

        // Assert Stock Release
        verify(inventoryReservationService).releaseStock("ITEM-1", 2);
    }
}
