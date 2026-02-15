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

        TenantConfigDTO config = TenantConfigDTO.builder()
                .name(tenant.getName())
                .logoUrl(tenant.getLogoUrl() != null ? tenant.getLogoUrl() : "/logo.png")
                .accentColor(tenant.getAccentColor() != null ? tenant.getAccentColor() : "#2563eb")
                .supportEmail(tenant.getSupportEmail() != null ? tenant.getSupportEmail() : "support@vaabhi.com")
                .heroVideoUrl(tenant.getHeroVideoUrl())
                .heroPosterUrl(tenant.getHeroPosterUrl())
                .socialLinks(tenant.getSocialLinks() != null ? tenant.getSocialLinks() : Map.of())
                .staticPages(tenant.getStaticPages() != null ? tenant.getStaticPages() : Map.of())
                .footerMenu(tenant.getFooterMenu() != null ? tenant.getFooterMenu() : Map.of())
                .build();

        return ResponseEntity.ok(ApiResponse.success(config, "Configuration retrieved successfully"));
    }
}
