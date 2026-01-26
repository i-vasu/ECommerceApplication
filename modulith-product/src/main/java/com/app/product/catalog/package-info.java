/**
 * Catalog Module - Product and Category Management
 * 
 * This module handles:
 * - Product CRUD operations
 * - Category management
 * - Product variants and customization
 * 
 * @see com.app.catalog.ProductServiceImpl
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "services", "repositories", "payloads",
        "entites", "config" })
package com.app.product.catalog;
