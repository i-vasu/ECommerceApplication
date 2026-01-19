package com.app.cart;

import com.app.order.entites.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCartService {

    private static final String CART_PREFIX = "cart:";
    // Cart persistence for 30 days
    private static final int CART_EXPIRY_DAYS = 30;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void addToCart(String userId, CartItem item) {
        String key = CART_PREFIX + userId;
        redisTemplate.opsForHash().put(key, item.getProductId(), item);
        redisTemplate.expire(key, CART_EXPIRY_DAYS, TimeUnit.DAYS);
    }

    public void removeFromCart(String userId, String productId) {
        String key = CART_PREFIX + userId;
        redisTemplate.opsForHash().delete(key, productId);
    }

    public List<CartItem> getCart(String userId) {
        String key = CART_PREFIX + userId;
        Map<Object, Object> items = redisTemplate.opsForHash().entries(key);
        List<CartItem> cartItems = new ArrayList<>();
        for (Object value : items.values()) {
            if (value instanceof CartItem) {
                cartItems.add((CartItem) value);
            }
        }
        return cartItems;
    }

    public void clearCart(String userId) {
        String key = CART_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
