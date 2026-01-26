package com.app.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryReservationService reservationService;

    @Override
    public InventoryLock lockInventory(String itemCode, int quantity) {
        boolean reserved = reservationService.reserveStock(itemCode, quantity);
        return new InventoryLock(reserved, reserved ? UUID.randomUUID().toString() : null);
    }

    @Override
    public InventoryLock lockFlashInventory(Long productId, int quantity) {
        // Implementation for flash inventory
        return new InventoryLock(true, UUID.randomUUID().toString()); // Mock for now
    }

    @Override
    public InventoryLock lockStock(java.util.List<com.app.inventory.payloads.InventoryRequest> requests) {
        // Multi-item stock locking
        boolean allReserved = true;
        String batchId = UUID.randomUUID().toString();
        for (var request : requests) {
            if (!reservationService.reserveStock(request.itemCode(), request.quantity())) {
                allReserved = false;
                break;
            }
        }
        return new InventoryLock(allReserved, allReserved ? batchId : null);
    }

    @Override
    public boolean checkAvailability(String itemCode, int quantity) {
        return reservationService.checkStock(itemCode, quantity);
    }
}
