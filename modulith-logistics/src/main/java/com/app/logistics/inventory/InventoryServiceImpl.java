package com.app.logistics.inventory;

import com.app.logistics.inventory.payloads.InventoryRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryReservationService reservationService;

    public InventoryServiceImpl(InventoryReservationService reservationService) {
        this.reservationService = reservationService;
    }

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
    public InventoryLock lockStock(List<InventoryRequest> requests) {
        // Multi-item stock locking
        boolean allReserved = true;
        String batchId = UUID.randomUUID().toString();
        for (InventoryRequest request : requests) {
            // Updated to use consistent method signature or logic if needed.
            // Assuming reserveStock supports batchId or similar multi-item logic.
            if (!reservationService.reserveStock(request.itemCode(), request.quantity())) {
                allReserved = false;
                break;
            }
        }
        return new InventoryLock(allReserved, allReserved ? batchId : null);
    }

    @Override
    public void confirmStock(String itemCode, int quantity, String lockId) {
        reservationService.confirmStock(itemCode, quantity, lockId);
    }

    @Override
    public boolean checkAvailability(String itemCode, int quantity) {
        return reservationService.checkStock(itemCode, quantity);
    }

    @Override
    public void releaseStock(String itemCode, int quantity) {
        reservationService.releaseStock(itemCode, quantity);
    }
}
