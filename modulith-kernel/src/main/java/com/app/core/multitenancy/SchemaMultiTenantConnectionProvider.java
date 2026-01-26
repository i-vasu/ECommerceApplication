package com.app.core.multitenancy;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;
import org.crac.Context;
import org.crac.Resource;
import org.crac.Core;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String>, Resource {

    private final DataSource dataSource;

    @jakarta.annotation.PostConstruct
    public void register() {
        Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        // Typically Hikari handles this if it's CRaC aware,
        // but for custom resources we ensure everything is clean.
        // If we held long-lived connections, we'd close them here.
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // Re-validation or re-initialization if needed.
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        // Switch search_path to the tenant's schema
        connection.createStatement().execute("SET search_path TO " + tenantIdentifier + ", public");
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        // Reset search_path to public
        connection.createStatement().execute("SET search_path TO public");
        releaseAnyConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
