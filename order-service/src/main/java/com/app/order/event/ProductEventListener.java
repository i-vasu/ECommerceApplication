package com.app.order.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.cart.CartService;
import com.app.repositories.CartRepo;
import com.app.entites.Cart;
import com.app.payloads.ProductEvent;
import java.util.List;

@Component
public class ProductEventListener {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private ObjectMapper objectMapper;

    public void handleMessage(String message) {
        try {
            ProductEvent event = objectMapper.readValue(message, ProductEvent.class);
            System.out.println("Received Redis Event: " + event);

            if ("UPDATED".equals(event.getEventType())) {
                List<Cart> carts = cartRepo.findCartsByProductId(event.getProductId());
                carts.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), event.getProductId()));
            } else if ("DELETED".equals(event.getEventType())) {
                List<Cart> carts = cartRepo.findCartsByProductId(event.getProductId());
                carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), event.getProductId()));
            }
        } catch (Exception e) {
            System.err.println("Error processing Redis message: " + e.getMessage());
        }
    }
}
