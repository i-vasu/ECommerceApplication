package com.app.core.events;

import java.time.LocalDateTime;

/**
 * Published when a user performs a product search.
 * 
 * Publishers: modulith-discovery (SearchService)
 * Listeners: modulith-marketing (track trends), modulith-support (detect gaps)
 * 
 * @param query       Search query
 * @param userId      User who performed search (null if anonymous)
 * @param resultCount Number of results found
 * @param timestamp   When search was performed
 */
public record ProductSearchEvent(
        String query,
        Long userId,
        int resultCount,
        LocalDateTime timestamp) {
    public ProductSearchEvent(String query, Long userId, int resultCount) {
        this(query, userId, resultCount, LocalDateTime.now());
    }
}
