package com.app.order.order;

import com.app.order.order.OrderServiceImpl;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.checkout.pipeline.OptimizedCheckoutService.CheckoutResult;
import com.app.order.async.OrderProducer;
import com.app.core.async.EventProducer;
import com.app.security.repositories.UserRepo;
import com.app.security.repositories.AddressRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.catalog.entities.Product;
import com.app.core.APIException;
import com.app.core.ResourceNotFoundException;
import com.app.core.services.RedisLockService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.rules.RuleEngineService;
import com.app.finance.payment.PaymentService;
import com.app.security.UserService;
import com.app.cart.domain.CartService;
import com.app.logistics.inventory.InventoryReservationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import org.springframework.context.ApplicationEventPublisher;
import com.app.order.repositories.*;
import com.app.finance.repositories.PaymentRepo;
import com.app.cart.repositories.*;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.finance.entities.Payment;
import com.app.order.mappers.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock private UserRepo userRepo;
    @Mock private CartRepo cartRepo;
    @Mock private OrderRepo orderRepo;
    @Mock private PaymentRepo paymentRepo;
    @Mock private OrderItemRepo orderItemRepo;
    @Mock private CartItemRepo cartItemRepo;
    @Mock private ProductRepo productRepo;
    @Mock private OrderMapper orderMapper;
    @Mock private OptimizedCheckoutService optimizedCheckoutService;
    @Mock private AddressRepo addressRepo;
    @Mock private OrderHistoryRepo orderHistoryRepo;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Tracer tracer;
    @Mock private OrderProducer orderProducer;
    @Mock private EventProducer eventProducer;
    @Mock private OperationalStateMachineService operationalStateMachine;
    @Mock private RuleEngineService ruleEngine;
    @Mock private RedisLockService lockService;
    @Mock private PaymentService paymentService;
    @Mock private UserService userService;
    @Mock private CartService cartService;
    @Mock private InventoryReservationService inventoryReservationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private String email = "test@example.com";
    private Long cartId = 1L;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testCart = new Cart();
        testCart.setCartId(cartId);
        testCart.setEmail(email);
        testCart.setTotalPrice(100.0);
        testCart.setCartItems(new ArrayList<>());
        
        CartItem item = new CartItem();
        item.setProductId(100L);
        item.setQuantity(1);
        item.setProductPrice(100.0);
        testCart.getCartItems().add(item);

        // Mock tracing for meterRegistry/tracer if necessary
    }

    @Test
    void testPlaceOrder_Success() {
        // Mock optimized checkout result
        CheckoutResult result = mock(CheckoutResult.class);
        when(result.inventory()).thenReturn(new CheckoutResult.InventoryStatus(true, "OK"));
        when(result.finalAmount()).thenReturn(110.0); // with tax/ship
        when(result.tax()).thenReturn(new CheckoutResult.TaxInfo(10.0, List.of()));
        when(result.shipping()).thenReturn(new CheckoutResult.ShippingInfo(0.0, "Standard"));
        
        when(cartRepo.findCartByEmailAndCartId(email, cartId)).thenReturn(testCart);
        when(optimizedCheckoutService.processCheckout(any(), any(), any())).thenReturn(result);
        
        // Mock lock
        when(lockService.tryLock(anyString(), any())).thenReturn(true);
        
        // Mock payment service
        com.app.finance.payloads.PaymentInitResponse payResponse = new com.app.finance.payloads.PaymentInitResponse(1L, "rzp_123", "created");
        when(paymentService.initiatePayment(anyLong(), anyString())).thenReturn(payResponse);
        
        // Mock Span for tracer
        Span span = mock(Span.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        
        orderService.placeOrder(email, cartId, "STRIPE");
        
        verify(orderRepo, atLeastOnce()).save(any(Order.class));
        verify(orderItemRepo).saveAll(anyList());
    }

    @Test
    void testPlaceOrder_InventoryFailure() {
        CheckoutResult result = mock(CheckoutResult.class);
        when(result.inventory()).thenReturn(new CheckoutResult.InventoryStatus(false, "Out of Stock"));
        
        when(cartRepo.findCartByEmailAndCartId(email, cartId)).thenReturn(testCart);
        when(optimizedCheckoutService.processCheckout(any(), any(), any())).thenReturn(result);
        
        assertThrows(APIException.class, () -> orderService.placeOrder(email, cartId, "COD"));
    }
}
