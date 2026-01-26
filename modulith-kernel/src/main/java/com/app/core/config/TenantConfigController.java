package com.app.core.config;

import com.app.core.config.payloads.TenantConfigDTO;
import com.app.core.multitenancy.Tenant;
import com.app.core.multitenancy.TenantManagementService;
import com.app.core.payloads.ApiResponse;
import com.app.core.version.ApiVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@ApiVersion(1)
@RequiredArgsConstructor
public class TenantConfigController {

    private final TenantManagementService tenantService;

    @GetMapping("/public/config")
    public ResponseEntity<ApiResponse<TenantConfigDTO>> getPublicConfig() {
        Tenant tenant = tenantService.getCurrentTenant();

        // In a real production app, these would be fields in the Tenant entity or a
        // separate CmsConfig entity.
        // For now, providing a robust integration point.
        TenantConfigDTO config = TenantConfigDTO.builder()
                .name(tenant.getName())
                .logoUrl("/logo.png")
                .accentColor("#2563eb")
                .supportEmail("support@vaabhi.com")
                .socialLinks(Map.of(
                        "instagram", "https://instagram.com/vaabhi",
                        "facebook", "https://facebook.com/vaabhi"))
                .staticPages(Map.of(
                        "privacy-policy", "<h1>Privacy Policy</h1><p>Your privacy is important to us...</p>",
                        "terms-of-service", "<h1>Terms of Service</h1><p>By using our service, you agree to...</p>"))
                .footerMenu(Map.of(
                        "Shop", List.of(
                                Map.of("title", "All Products", "url", "/search"),
                                Map.of("title", "Collections", "url", "/search")),
                        "Company", List.of(
                                Map.of("title", "Privacy Policy", "url", "/privacy-policy"),
                                Map.of("title", "Terms of Service", "url", "/terms-of-service"))))
                .build();

        return ResponseEntity.ok(ApiResponse.success(config, "Configuration retrieved successfully"));
    }
}
