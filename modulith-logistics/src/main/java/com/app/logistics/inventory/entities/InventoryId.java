package com.app.logistics.inventory.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryId implements Serializable {

    private String itemCode;
    private Long warehouseId;
    private Long binId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryId that = (InventoryId) o;
        return Objects.equals(itemCode, that.itemCode) &&
                Objects.equals(warehouseId, that.warehouseId) &&
                Objects.equals(binId, that.binId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemCode, warehouseId, binId);
    }
}
