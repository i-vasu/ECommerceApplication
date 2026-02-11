package com.app.checkout.pipeline;

import com.app.core.contracts.CartContract;
import com.app.governance.states.InventoryEvent;
import com.app.governance.states.OperationalStateMachineService;
import com.app.logistics.inventory.InventoryService;
import com.app.logistics.inventory.InventoryService.InventoryLock;
import com.app.logistics.inventory.payloads.InventoryRequest;
import com.app.security.entities.Address;
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
    public InventoryLock execute(CartContract cart, Address address) {
        java.util.List<InventoryRequest> requests = cart.items().stream()
                .map(item -> new InventoryRequest(item.itemCode(),
                        item.quantity()))
                .toList();
        InventoryLock lock = inventoryService.lockStock(requests);
        if (lock.locked()) {
            stateMachineService.triggerInventoryEvent(0L, InventoryEvent.RESERVE);
        }
        return lock;
    }
}
