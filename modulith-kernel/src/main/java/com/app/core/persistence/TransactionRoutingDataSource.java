package com.app.core.persistence;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Robust Read/Write Splitting Router.
 * 
 * In a real-world high-traffic fashion store:
 * 1. Writes go to the 'Master' instance (ACID).
 * 2. Reads go to 'Replica' instances.
 * 
 * This prevents intensive search/reporting queries from blocking
 * critical checkout transactions.
 */
public class TransactionRoutingDataSource extends AbstractRoutingDataSource {

    public enum DataSourceType {
        MASTER, REPLICA
    }

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    public static void setReadonly(boolean isReadonly) {
        CONTEXT.set(isReadonly ? DataSourceType.REPLICA : DataSourceType.MASTER);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return CONTEXT.get();
    }
}
