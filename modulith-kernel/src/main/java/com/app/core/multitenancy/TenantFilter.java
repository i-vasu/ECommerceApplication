package com.app.core.multitenancy;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1) // Ensure it runs early
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String tenantId = req.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "public";
        }

        try {
            // Java 25 ScopedValue Usage
            TenantContext.callWithTenant(tenantId, () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (Exception e) {
            if (e instanceof IOException)
                throw (IOException) e;
            if (e instanceof ServletException)
                throw (ServletException) e;
            throw new ServletException(e);
        }
    }
}
