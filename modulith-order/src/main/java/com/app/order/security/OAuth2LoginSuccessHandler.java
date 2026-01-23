package com.app.order.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.app.core.security.JWTUtil;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.config.AppConstants;
import com.app.identity.entities.Role;
import com.app.identity.entities.User;
import com.app.identity.repositories.RoleRepo;
import com.app.identity.repositories.UserRepo;
import com.app.order.services.ERPNextService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private ERPNextService erpNextService;

    @Value("${frontend.url:http://localhost:3000/oauth2/redirect}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture"); // Google
        if (picture == null) {
            picture = oauth2User.getAttribute("avatar_url"); // GitHub
        }

        if (email == null) {
            // Fallback for Github if 'email' is private/null, use login with mock domain
            String login = oauth2User.getAttribute("login");
            if (login != null) {
                email = login + "@github.com";
            } else {
                // Error or fallback
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found in OAuth provider");
                return;
            }
        }

        User user = userRepo.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            // Split name if possible
            if (name != null) {
                String[] parts = name.split(" ");
                user.setFirstName(parts[0]);
                if (parts.length > 1) {
                    user.setLastName(parts[parts.length - 1]);
                } else {
                    user.setLastName("");
                }
            } else {
                user.setFirstName("User");
                user.setLastName("");
            }

            user.setPassword(""); // Empty password for OAuth users
            user.setVerified(true);
            user.setAvatarUrl(picture);

            Role userRole = roleRepo.findById(AppConstants.USER_ID).orElse(null);
            if (userRole != null) {
                user.setRoles(Set.of(userRole));
            }

            userRepo.save(user);
            erpNextService.createCustomer(user);
        }

        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String token = jwtUtil.generateToken(email, tenantId);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
