package com.app.checkout.pipeline;

import com.app.cart.entities.Cart;
import com.app.security.entities.Address;

/**
 * Enterprise-grade Checkout Activity.
 * Mirrors Broadleaf's Activity pattern but optimized for Java 25.
 */
public interface CheckoutActivity<T> {
    String getName();

    T execute(Cart cart, Address address);
}
