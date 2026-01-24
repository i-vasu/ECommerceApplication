package com.app.commerce.checkout;

import com.app.order.payloads.OrderDTO;
import com.app.cart.CartService;
import com.app.commerce.pricing.OrderTotalService;
import com.app.inventory.InventoryReservationService;
import com.app.payment.PaymentService;
import com.app.order.repositories.OrderRepo;
import com.app.order.repositories.CartRepo;
import com.app.order.repositories.OrderItemRepo;
import com.app.product.repositories.ProductRepo;
import com.app.order.entities.Cart;
import com.app.order.entities.CartItem;
import com.app.order.entities.Order;
import com.app.order.entities.OrderItem;
import com.app.order.entities.Payment;
import com.app.order.mappers.OrderMapper;
import com.app.order.OrderCreatedEvent;
import com.app.commerce.pricing.contracts.OrderSummary;
import com.app.commerce.states.OrderStatus;
import com.app.core.ResourceNotFoundException;
import com.app.core.APIException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Orchestrator for the Transactional Checkout Flow.
 * Facades the complexity of Pricing, Inventory, and Payment.
 */
@Log4j2
@RequiredArgsConstructor
@Service
public class CheckoutService {

    private final CartService cartService;
    private final OrderTotalService orderTotalService;
    private final InventoryReservationService inventoryService;
    private final OrderRepo orderRepo;
    private final CartRepo cartRepo;
    private final ProductRepo productRepo;
    private final OrderItemRepo orderItemRepo;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Step 1: Initialize Checkout (Validate Cart & Stock)
     */
    @Transactional
    public void startCheckout(Long cartId) {
        var cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cannot checkout an empty cart");
        }

        for (var item : cart.getCartItems()) {
            if (!inventoryService.checkStock(item.getItemCode(), item.getQuantity())) {
                throw new APIException("Insufficient stock for item: " + item.getItemCode());
            }
        }
    }

    /**
     * Step 2: Set Shipping Address & Calculate Taxes/Shipping
     */
    @Transactional
    public void setShippingAddress(Long cartId, Long addressId) {
        var cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        cart.setAddressId(addressId);

        var summary = orderTotalService.calculate(cart);
        cart.setTotalPrice(summary.getFinalTotal().doubleValue());
        cartRepo.save(cart);
    }

    /**
     * Step 2.1: Apply Coupon Code
     */
    @Transactional
    public void applyCoupon(Long cartId, String couponCode) {
        cartService.applyCoupon(cartId, couponCode);
    }

    /**
     * Step 3: Apply Payment & Confirm
     */
    @Transactional
    public OrderDTO confirmOrder(Long cartId, String paymentMethod) {
        var cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }

        List<CartItem> reservedItems = new ArrayList<>();
        try {
            for (var item : cart.getCartItems()) {
                if (!inventoryService.reserveStock(item.getItemCode(), item.getQuantity())) {
                    for (var r : reservedItems) {
                        inventoryService.releaseStock(r.getItemCode(), r.getQuantity());
                    }
                    throw new APIException("Out of stock for item: " + item.getItemCode());
                }
                reservedItems.add(item);
            }
        } catch (Exception e) {
            for (var r : reservedItems) {
                inventoryService.releaseStock(r.getItemCode(), r.getQuantity());
            }
            throw e;
        }

        try {
            var summary = orderTotalService.calculate(cart);

            var order = new Order();
            order.setEmail(cart.getUser() != null ? cart.getUser().getEmail() : "guest");
            order.setOrderDate(LocalDate.now());
            order.setTotalAmount(summary.getTotal());
            order.setOrderStatus(OrderStatus.PENDING);
            order.setCouponCode(cart.getCouponCode());
            order.setDiscountAmount(Math.abs(summary.getDiscountTotal()));

            var payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentMethod(paymentMethod);
            order.setPayment(payment);

            var savedOrder = orderRepo.save(order);

            List<OrderItem> orderItems = new ArrayList<>();
            for (var cartItem : cart.getCartItems()) {
                var orderItem = new OrderItem();
                orderItem.setProduct(productRepo.findById(cartItem.getProductId()).orElseThrow());
                orderItem.setItemCode(cartItem.getItemCode());
                orderItem.setProductName(cartItem.getProductName());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setDiscount(cartItem.getDiscount());
                orderItem.setOrderedPrice(cartItem.getProductPrice());
                orderItem.setOrder(savedOrder);
                orderItems.add(orderItem);
            }
            orderItemRepo.saveAll(orderItems);
            savedOrder.setOrderItems(orderItems);

            for (var item : new ArrayList<>(cart.getCartItems())) {
                cartService.deleteProductFromCart(cartId, item.getProductId());
            }

            eventPublisher.publishEvent(new OrderCreatedEvent(
                    savedOrder.getOrderId(),
                    savedOrder.getEmail(),
                    BigDecimal.valueOf(savedOrder.getTotalAmount())));

            return orderMapper.orderToOrderDTO(savedOrder);

        } catch (Exception e) {
            for (var r : reservedItems) {
                inventoryService.releaseStock(r.getItemCode(), r.getQuantity());
            }
            log.error("Checkout failed for cart {}: {}", cartId, e.getMessage());
            throw new APIException("Checkout failed: " + e.getMessage());
        }
    }
}
