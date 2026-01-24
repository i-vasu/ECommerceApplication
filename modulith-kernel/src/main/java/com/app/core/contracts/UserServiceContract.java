package com.app.core.contracts;

/**
 * User Service Contract - Exposes user operations to other modules
 * This interface allows other modules to interact with the Identity module
 * without direct dependencies on implementation classes.
 * 
 * Note: Uses primitive types and Strings to avoid DTO dependencies
 */
public interface UserServiceContract {

    /**
     * Check if user exists
     */
    boolean userExists(Long userId);

    /**
     * Check if user exists by email
     */
    boolean userExistsByEmail(String email);

    /**
     * Get user email by ID
     */
    String getUserEmail(Long userId);

    /**
     * Get user full name by ID
     */
    String getUserFullName(Long userId);
}
