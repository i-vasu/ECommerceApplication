package com.app.logistics.shipping.payloads;

public record InventoryRequest(String itemCode, int quantity) {
}
