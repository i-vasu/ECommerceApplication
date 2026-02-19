package com.app.core.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Centralized JWT Filter for the Modulith platform.
 */
@Component
public class JWTFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired(required = false)
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank() && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                JWTUtil.TokenData tokenData = jwtUtil.decodeToken(jwt);
                String email = tokenData.email();
                String tokenTenantId = tokenData.tenantId();
                String currentTenantId = com.app.core.multitenancy.TenantContext.getTenantId();

                // Cross-Tenant Validation
                if (tokenTenantId != null && !tokenTenantId.equals(currentTenantId)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-tenant access denied");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken;
                if (userDetailsService != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    authToken = new UsernamePasswordAuthenticationToken(email,
                            userDetails.getPassword(), userDetails.getAuthorities());
                } else {
                    authToken = new UsernamePasswordAuthenticationToken(email, null, java.util.Collections.emptyList());
                }

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (JWTVerificationException exc) {
                // Invalid Token
                logger.error("JWT Login Verification ",exc);
            }
        }
        filterChain.doFilter(request, response);
    }
}
