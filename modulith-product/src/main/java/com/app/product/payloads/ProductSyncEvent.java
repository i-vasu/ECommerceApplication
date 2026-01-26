package com.app.product.payloads;

import java.io.Serializable;

public class ProductSyncEvent implements Serializable {
    private String itemCode;
    private String productName;
    private String status; // CREATED, UPDATED, DELETED

    public ProductSyncEvent() {
    }

    private Long productId; // Added to support ID-based events
    private String tenantId;

    public ProductSyncEvent(String itemCode, String productName, String status, String tenantId) {
        this.itemCode = itemCode;
        this.productName = productName;
        this.status = status;
        this.tenantId = tenantId;
    }

    public ProductSyncEvent(Long productId, String status, String tenantId) {
        this.productId = productId;
        this.status = status;
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
