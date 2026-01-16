package com.app.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import com.auth0.jwt.exceptions.JWTVerificationException;
import java.lang.reflect.Field;

public class SecurityTest {

    private JWTFilter jwtFilter;
    private JWTUtil jwtUtil;

    @BeforeEach
    public void setup() throws Exception {
        jwtUtil = mock(JWTUtil.class);
        jwtFilter = new JWTFilter();

        // Inject mock JWTUtil into JWTFilter using reflection (since field injection is
        // used)
        Field jwtUtilField = JWTFilter.class.getDeclaredField("jwtUtil");
        jwtUtilField.setAccessible(true);
        jwtUtilField.set(jwtFilter, jwtUtil);

        SecurityContextHolder.clearContext();
    }

    @Test
    public void testValidTokenAuthentication() throws Exception {
        String validToken = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + validToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtil.validateTokenAndRetrieveSubject(validToken)).thenReturn("user@example.com");

        jwtFilter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(
                SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken);
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getName().equals("user@example.com"));
    }

    @Test
    public void testInvalidTokenRejected() throws Exception {
        String invalidToken = "invalid.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtil.validateTokenAndRetrieveSubject(invalidToken)).thenThrow(new JWTVerificationException("Invalid"));

        jwtFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // Response status should be set to Forbidden (403) manually in filter catch
        // block
        assertTrue(response.getStatus() == 403);
    }
}
