package com.app.inventory;

import com.app.order.entities.Cart;

public interface InventoryService {
    InventoryLock lockInventoryForCart(Cart cart);

    boolean checkAvailability(Cart cart);

    public record InventoryLock(boolean locked, String lockId) {
    }
}
