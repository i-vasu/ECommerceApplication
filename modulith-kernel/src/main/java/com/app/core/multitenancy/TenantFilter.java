package com.app.core.multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1) // Ensure it runs early
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private final TenantRepository tenantRepository;
    
    public TenantFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String tenantId = req.getHeader(TENANT_HEADER);

        // Fallback to cookie for UI sessions
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = getCookie(req, "TENANT_ID");
        }

        // Allow public endpoints (e.g. actuators, login, registration) to pass without tenant check if needed
        // but generally, we default to 'public' schema or block.
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "public";
        }

        // Validate existence if not public
        if (!"public".equals(tenantId)) {
            boolean exists = tenantRepository.findByTenantId(tenantId).isPresent();
            if (!exists) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Tenant ID: " + tenantId);
                return;
            }
        }

        try {
            // Java 25 ScopedValue Usage
            String finalTenantId = tenantId;
            TenantContext.callWithTenant(finalTenantId, () -> {
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
    private String getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
