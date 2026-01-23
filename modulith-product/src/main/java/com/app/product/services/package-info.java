/**
 * Services Module - Core Business Logic
 * 
 * This module contains:
 * - Image processing (vips-ffm)
 * - Semantic search (DJL embeddings)
 * - Product caching
 * 
 * @see com.app.services.ImageService
 * @see com.app.services.SemanticSearchService
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "repositories", "entites", "payloads" })
package com.app.product.services;
