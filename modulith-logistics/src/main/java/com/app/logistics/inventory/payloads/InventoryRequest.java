package com.app.logistics.inventory.payloads;

public record InventoryRequest(String itemCode, int quantity) {
}
