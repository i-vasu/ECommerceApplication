package com.app.order.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncEvent implements Serializable {
    private String itemCode;
    private String productName;
    private String status; // CREATED, UPDATED, DELETED, SYNC_COMPLETE
}
