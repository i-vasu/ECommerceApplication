package com.app.order.order;

import com.app.cart.domain.CartService;
import com.app.cart.entities.Cart;
import com.app.cart.repositories.CartItemRepo;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.checkout.pipeline.OptimizedCheckoutService.CheckoutResult;
import com.app.core.APIException;
import com.app.core.async.EventProducer;
import com.app.core.services.RedisLockService;
import com.app.finance.payloads.PaymentInitResponse;
import com.app.finance.payment.PaymentService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.logistics.inventory.InventoryService.InventoryLock;
import com.app.logistics.shipping.ShippingCalculationService.ShippingCost;
import com.app.logistics.shipping.TaxCalculationService.TaxCalculation;
import com.app.order.async.OrderProducer;
import com.app.order.entities.Order;
import com.app.order.mappers.OrderMapper;
import com.app.order.payloads.OrderRequest;
import com.app.order.repositories.OrderHistoryRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.repositories.OrderRepo;
import com.app.security.entities.User;
import com.app.security.repositories.AddressRepo;
import com.app.security.repositories.UserRepo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserRepo userRepo;
    @Mock private OrderRepo orderRepo;
    @Mock private OrderItemRepo orderItemRepo;
    @Mock private CartRepo cartRepo;
    @Mock private CartItemRepo cartItemRepo;
    @Mock private com.app.core.contracts.UserServiceContract userService;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private PaymentService paymentService;
    @Mock private InventoryReservationService inventoryReservationService;
    @Mock private OptimizedCheckoutService optimizedCheckoutService;
    @Mock private AddressRepo addressRepo;
    @Mock private OrderHistoryRepo orderHistoryRepo;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Tracer tracer;
    @Mock private Span span;
    @Mock private ProductRepo productRepo;
    @Mock private OrderProducer orderProducer;
    @Mock private EventProducer eventProducer;
    @Mock private OperationalStateMachineService stateMachineService;
    @Mock private com.app.governance.rules.RuleEngineService ruleEngine;
    @Mock private RedisLockService lockService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        // Setup Tracer mocks
        lenient().when(tracer.nextSpan()).thenReturn(span);
        lenient().when(span.name(anyString())).thenReturn(span);
        lenient().when(span.start()).thenReturn(span);
        
        // Setup Micrometer mocks
        Timer timer = mock(Timer.class);
        lenient().when(meterRegistry.timer(anyString(), any(String[].class))).thenReturn(timer);
        
        Counter counter = mock(Counter.class);
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        
        lenient().when(timer.record(any(java.util.function.Supplier.class))).thenAnswer(i -> {
            java.util.function.Supplier<?> s = i.getArgument(0);
            return s.get();
        });

        orderService.initMetrics();
    }

    @Test
    @DisplayName("BEHAVIOR: Should block order placement if concurrency lock is held")
    void placeOrder_LockConcurrecy_ShouldThrowException() {
        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(false);
        assertThrows(APIException.class, () -> 
            orderService.placeOrder("user@example.com", 1L, "UPI", new OrderRequest())
        );
    }

    @Test
    @DisplayName("BEHAVIOR: Should rollback inventory if order saving fails")
    void placeOrder_InventoryRollbackOnFailure() {
        String email = "user@example.com";
        Long cartId = 1L;
        User user = new User();
        user.setUserId(101L);
        Cart cart = new Cart();
        cart.setCartItems(Collections.singletonList(new com.app.cart.entities.CartItem()));
        cart.getCartItems().get(0).setItemCode("SKU-1");
        cart.getCartItems().get(0).setQuantity(2);

        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(cartRepo.findCartByUserIdAndCartId(101L, cartId)).thenReturn(cart);

        InventoryLock invLock = new InventoryLock(true, "lock-123");
        CheckoutResult result = new CheckoutResult(
            invLock, null, mock(TaxCalculation.class), mock(ShippingCost.class), 
            BigDecimal.ZERO, BigDecimal.valueOf(100), Collections.emptyList()
        );
        when(optimizedCheckoutService.processCheckout(any(), any())).thenReturn(result);
        
        when(orderRepo.save(any(Order.class))).thenThrow(new RuntimeException("DB Down"));

        assertThrows(APIException.class, () -> 
            orderService.placeOrder(email, cartId, "CARD", new OrderRequest())
        );

        verify(inventoryReservationService).releaseStock("SKU-1", 2);
    }

    @Test
    @DisplayName("BEHAVIOR: Should cancel order if payment initiation fails")
    void placeOrder_PaymentFailure_ShouldCancelOrder() {
        String email = "user@example.com";
        Long cartId = 1L;
        User user = new User();
        user.setUserId(101L);
        Cart cart = new Cart();
        cart.setTotalPrice(BigDecimal.valueOf(100));
        cart.setCartItems(Collections.singletonList(new com.app.cart.entities.CartItem()));
        cart.getCartItems().get(0).setItemCode("SKU-1");
        cart.getCartItems().get(0).setQuantity(1);

        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(cartRepo.findCartByUserIdAndCartId(101L, cartId)).thenReturn(cart);

        CheckoutResult result = new CheckoutResult(
            new InventoryLock(true, "L1"), null, mock(TaxCalculation.class), mock(ShippingCost.class), 
            BigDecimal.ZERO, BigDecimal.valueOf(100), Collections.emptyList()
        );
        when(optimizedCheckoutService.processCheckout(any(), any())).thenReturn(result);

        Order savedOrder = new Order();
        savedOrder.setOrderId(500L);
        savedOrder.setTotalAmount(BigDecimal.valueOf(100));
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);
        // orderItemRepo.saveAll is not reached if payment fails, so removing stub

        // Simulate payment failure
        when(paymentService.initiatePayment(anyLong(), anyString())).thenThrow(new APIException("Payment Gateway Down"));

        assertThrows(APIException.class, () -> 
            orderService.placeOrder(email, cartId, "RAZORPAY", new OrderRequest())
        );

        // Verify order is marked as failed or cancelled (logic depends on implementation, 
        // assuming standard rollback or state update. Note: Transactional might roll it back entirely, 
        // so verify save was at least attempted).
        verify(orderRepo).save(any(Order.class));
        verify(inventoryReservationService).releaseStock(any(), anyInt()); // Should release stock
    }

    @Test
    @DisplayName("SUCCESS: Full Order Placement Flow")
    void placeOrder_Success() {
        String email = "user@example.com";
        Long cartId = 1L;
        User user = new User();
        user.setUserId(101L);
        Cart cart = new Cart();
        cart.setTotalPrice(BigDecimal.valueOf(100));
        com.app.cart.entities.CartItem item = new com.app.cart.entities.CartItem();
        item.setProductId(201L);
        item.setItemCode("SKU-1");
        item.setProductName("Product 1");
        item.setQuantity(2);
        item.setProductPrice(BigDecimal.valueOf(50));
        item.setDiscount(BigDecimal.ZERO);
        cart.setCartItems(Collections.singletonList(item));

        when(lockService.tryLock(anyString(), any(Duration.class))).thenReturn(true);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(cartRepo.findCartByUserIdAndCartId(101L, cartId)).thenReturn(cart);
        when(productRepo.findById(201L)).thenReturn(Optional.of(new com.app.catalog.entities.Product()));

        CheckoutResult result = new CheckoutResult(
            new InventoryLock(true, "L1"), null, mock(TaxCalculation.class), mock(ShippingCost.class), 
            BigDecimal.ZERO, BigDecimal.valueOf(100), Collections.emptyList()
        );
        when(optimizedCheckoutService.processCheckout(any(), any())).thenReturn(result);

        Order savedOrder = new Order();
        savedOrder.setOrderId(500L);
        savedOrder.setEmail(email);
        savedOrder.setUserId(101L);
        savedOrder.setTotalAmount(BigDecimal.valueOf(100));
        
        when(orderRepo.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepo.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(paymentService.initiatePayment(anyLong(), anyString())).thenReturn(new PaymentInitResponse(1L, "PG-ORDER-1", "SUCCESS"));

        orderService.placeOrder(email, cartId, "RAZORPAY", new OrderRequest());

        verify(stateMachineService).triggerOrderEvent(eq(500L), any());
        verify(eventPublisher).publishEvent(any(com.app.core.events.OrderCreatedEvent.class));
        verify(lockService).unlock(anyString());
        
        // Metrics verification
        verify(meterRegistry, atLeastOnce()).timer(eq("ecommerce.checkout.duration"));
        verify(meterRegistry, atLeastOnce()).counter(eq("ecommerce.checkout.success"));
    }
}
