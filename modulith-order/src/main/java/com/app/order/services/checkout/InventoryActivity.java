package com.app.order.services.checkout;

import com.app.order.entities.Cart;
import com.app.identity.entities.Address;
import com.app.inventory.InventoryService;
import com.app.inventory.InventoryService.InventoryLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryActivity implements CheckoutActivity<InventoryLock> {

    private final InventoryService inventoryService;

    @Override
    public String getName() {
        return "inventory-lock";
    }

    @Override
    public InventoryLock execute(Cart cart, Address address) {
        java.util.List<com.app.inventory.payloads.InventoryRequest> requests = cart.getCartItems().stream()
                .map(item -> new com.app.inventory.payloads.InventoryRequest(item.getItemCode(), item.getQuantity()))
                .toList();
        return inventoryService.lockStock(requests);
    }
}
