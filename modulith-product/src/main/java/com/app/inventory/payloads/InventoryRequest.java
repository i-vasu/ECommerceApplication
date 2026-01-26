package com.app.inventory.payloads;

public record InventoryRequest(String itemCode, int quantity) {
}
