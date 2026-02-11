package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
import com.app.security.entities.Address;

/**
 * Enterprise-grade Checkout Activity.
 * Mirrors Broadleaf's Activity pattern but optimized for Java 17+.
 */
public interface CheckoutActivity<T> {
    String getName();

    T execute(CartContract cart, Address address);
}
