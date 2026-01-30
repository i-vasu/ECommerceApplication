package com.app.logistics.shipping;

public interface InventoryService {
    InventoryLock lockInventory(String itemCode, int quantity);

    InventoryLock lockFlashInventory(Long productId, int quantity);

    InventoryLock lockStock(java.util.List<com.app.logistics.inventory.payloads.InventoryRequest> requests);

    boolean checkAvailability(String itemCode, int quantity);

    void confirmStock(String itemCode, int quantity, String lockId);

    public record InventoryLock(boolean locked, String lockId) {
    }
}
