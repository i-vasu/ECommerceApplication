package com.app.core.multitenancy;

/**
 * Thread-safe context for holding the current user identifier and email.
 * Follows the ScopedValue pattern established in TenantContext.
 */
public class UserContext {

    public static final ScopedValue<Long> USER_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> USER_EMAIL = ScopedValue.newInstance();

    /**
     * Get the current user ID. Returns null if no user is bound to the current thread.
     */
    public static Long getCurrentUserId() {
        return USER_ID.isBound() ? USER_ID.get() : null;
    }

    /**
     * Get the current user email. Returns null if no user is bound to the current thread.
     */
    public static String getCurrentUserEmail() {
        return USER_EMAIL.isBound() ? USER_EMAIL.get() : null;
    }

    /**
     * Run a task within the scope of a user.
     */
    public static void runWithUser(Long userId, String email, Runnable task) {
        ScopedValue.where(USER_ID, userId)
                  .where(USER_EMAIL, email)
                  .run(task);
    }

    /**
     * Call a Callable within the scope of a user.
     */
    public static <T> T callWithUser(Long userId, String email, java.util.concurrent.Callable<T> task) throws Exception {
        return ScopedValue.where(USER_ID, userId)
                          .where(USER_EMAIL, email)
                          .call(task::call);
    }
}
