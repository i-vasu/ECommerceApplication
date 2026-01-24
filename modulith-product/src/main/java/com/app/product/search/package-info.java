/**
 * Search Module - Visual and Semantic Search
 * 
 * This module handles:
 * - Product data flow from ERPNext
 * - AI-powered image embeddings
 * - Similarity search
 * 
 * @see com.app.search.ProductDataFlowService
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "services", "repositories", "entites" })
package com.app.product.search;
