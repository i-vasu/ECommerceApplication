/**
 * Inventory Module - ERPNext Integration
 * 
 * This module handles:
 * - ERPNext webhook processing
 * - Product sync from ERPNext
 * - Stock management
 * 
 * @see com.app.inventory.ERPNextProductSyncService
 * @see com.app.inventory.ERPNextWebhookController
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "services", "repositories", "entites",
        "search" })
package com.app.inventory;
