package com.app.order.order;

import com.app.cart.domain.CartService;
import com.app.cart.entities.Cart;
import com.app.cart.entities.CartItem;
import com.app.cart.repositories.CartItemRepo;
import com.app.cart.repositories.CartRepo;
import com.app.catalog.repositories.ProductRepo;
import com.app.checkout.pipeline.OptimizedCheckoutService;
import com.app.checkout.pipeline.OptimizedCheckoutService.CheckoutResult;
import com.app.core.APIException;
import com.app.core.async.EventProducer;
import com.app.core.services.RedisLockService;
import com.app.finance.payment.PaymentService;
import com.app.finance.repositories.PaymentRepo;
import com.app.governance.rules.RuleEngineService;
import com.app.governance.states.OperationalStateMachineService;
import com.app.logistics.inventory.InventoryReservationService;
import com.app.order.async.OrderProducer;
import com.app.order.entities.Order;
import com.app.order.mappers.OrderMapper;
import com.app.order.repositories.OrderHistoryRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.order.repositories.OrderRepo;
import com.app.security.repositories.AddressRepo;
import com.app.security.repositories.UserRepo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock private com.app.core.contracts.UserServiceContract userService;
    @Mock private CartRepo cartRepo;
    @Mock private OrderRepo orderRepo;
    @Mock private PaymentRepo paymentRepo;
    @Mock private OrderItemRepo orderItemRepo;
    @Mock private CartItemRepo cartItemRepo;
    @Mock private UserRepo userRepo;
    @Mock private ProductRepo productRepo;
    @Mock private OrderMapper orderMapper;
    @Mock private OptimizedCheckoutService optimizedCheckoutService;
    @Mock private AddressRepo addressRepo;
    @Mock private OrderHistoryRepo orderHistoryRepo;
    private MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    @Mock private Tracer tracer;
    @Mock private OrderProducer orderProducer;
    @Mock private EventProducer eventProducer;
    @Mock private OperationalStateMachineService operationalStateMachine;
    @Mock private RuleEngineService ruleEngine;
    @Mock private RedisLockService lockService;
    @Mock private PaymentService paymentService;
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
        com.app.security.entities.User user = new com.app.security.entities.User();
        user.setUserId(200L);
        user.setEmail(email);
        testCart.setUserId(user.getUserId());
        testCart.setTotalPrice(BigDecimal.valueOf(100.0));
        testCart.setCartItems(new ArrayList<>());
        
        CartItem item = new CartItem();
        item.setProductId(100L);
        item.setQuantity(1);
        item.setProductPrice(BigDecimal.valueOf(100.0));
        testCart.getCartItems().add(item);

        org.springframework.test.util.ReflectionTestUtils.setField(orderService, "meterRegistry", meterRegistry);
        orderService.initMetrics();
        
        // Mock Tracer/Span
        Span span = mock(Span.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.start()).thenReturn(span);
        
        when(lockService.tryLock(anyString(), any())).thenReturn(true);
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(cartRepo.findCartByUserIdAndCartId(anyLong(), anyLong())).thenReturn(testCart);

        // Stub orderMapper leniently
        lenient().when(orderMapper.orderToOrderDTO(any())).thenReturn(mock(com.app.order.payloads.OrderDTO.class));
    }

    @Test
    void testPlaceOrder_Success() {
        // Mock optimized checkout result
        com.app.logistics.inventory.InventoryService.InventoryLock invLock = mock(com.app.logistics.inventory.InventoryService.InventoryLock.class);
        when(invLock.locked()).thenReturn(true);
        
        CheckoutResult result = new CheckoutResult(
            invLock,
            mock(com.app.security.AddressValidationService.AddressValidation.class),
            mock(com.app.logistics.shipping.TaxCalculationService.TaxCalculation.class),
            mock(com.app.logistics.shipping.ShippingCalculationService.ShippingCost.class),
            BigDecimal.ZERO,
            BigDecimal.valueOf(110.0),
            new ArrayList<>()
        );
        
        when(optimizedCheckoutService.processCheckout(any(), any())).thenReturn(result);
        
        // Mock product lookup
        com.app.catalog.entities.Product dummyProduct = mock(com.app.catalog.entities.Product.class);
        when(productRepo.findById(100L)).thenReturn(Optional.of(dummyProduct));
        
        // Mock order save to return an order with an ID
        lenient().when(orderRepo.save(any())).thenAnswer(invocation -> {
            com.app.order.entities.Order order = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(order, "orderId", 1L);
            return order;
        });
        
        lenient().when(orderItemRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock payment service
        com.app.finance.payloads.PaymentInitResponse payResponse = new com.app.finance.payloads.PaymentInitResponse(1L, "rzp_123", "created");
        when(paymentService.initiatePayment(anyLong(), anyString())).thenReturn(payResponse);
        
        orderService.placeOrder(email, cartId, "STRIPE");
        
        verify(orderRepo, atLeastOnce()).save(any(Order.class));
        verify(orderItemRepo).saveAll(anyList());
    }

    @Test
    void testPlaceOrder_InventoryFailure() {
        CheckoutResult result = new CheckoutResult(
            new com.app.logistics.inventory.InventoryService.InventoryLock(false, "Out of stock"),
            mock(com.app.security.AddressValidationService.AddressValidation.class),
            mock(com.app.logistics.shipping.TaxCalculationService.TaxCalculation.class),
            mock(com.app.logistics.shipping.ShippingCalculationService.ShippingCost.class),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new ArrayList<>()
        );
        
        when(optimizedCheckoutService.processCheckout(any(), any())).thenReturn(result);
        
        assertThrows(APIException.class, () -> orderService.placeOrder(email, cartId, "COD"));
    }
}
