package com.app.order.services.checkout;

import com.app.order.entities.Cart;
import com.app.identity.entities.Address;

/**
 * Enterprise-grade Checkout Activity.
 * Mirrors Broadleaf's Activity pattern but optimized for Java 25.
 */
public interface CheckoutActivity<T> {
    String getName();

    T execute(Cart cart, Address address);
}
