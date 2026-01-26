package com.app.inventory;

public interface InventoryService {
    InventoryLock lockInventory(String itemCode, int quantity);

    InventoryLock lockFlashInventory(Long productId, int quantity);

    InventoryLock lockStock(java.util.List<com.app.inventory.payloads.InventoryRequest> requests);

    boolean checkAvailability(String itemCode, int quantity);

    public record InventoryLock(boolean locked, String lockId) {
    }
}
