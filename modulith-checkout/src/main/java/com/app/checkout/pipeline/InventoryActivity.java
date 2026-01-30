package com.app.checkout.pipeline;

import com.app.cart.entities.Cart;
import com.app.security.entities.Address;
import com.app.logistics.inventory.InventoryService;
import com.app.logistics.inventory.InventoryService.InventoryLock;
import com.app.logistics.inventory.payloads.InventoryRequest;
import com.app.governance.states.OperationalStateMachineService;
import com.app.governance.states.InventoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryActivity implements CheckoutActivity<InventoryLock> {

    private final InventoryService inventoryService;
    private final OperationalStateMachineService stateMachineService;

    @Override
    public String getName() {
        return "inventory-lock";
    }

    @Override
    public InventoryLock execute(Cart cart, Address address) {
        java.util.List<InventoryRequest> requests = cart.getCartItems().stream()
                .map(item -> new InventoryRequest(item.getItemCode(),
                        item.getQuantity()))
                .toList();
        InventoryLock lock = inventoryService.lockStock(requests);
        if (lock.locked()) {
            stateMachineService.triggerInventoryEvent(0L, InventoryEvent.RESERVE);
        }
        return lock;
    }
}
