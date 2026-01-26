package com.app.core.config.payloads;

import java.util.Map;
import lombok.Builder;

@Builder
public record TenantConfigDTO(
        String name,
        String logoUrl,
        String accentColor,
        String supportEmail,
        Map<String, String> socialLinks,
        Map<String, String> staticPages,
        Map<String, Object> footerMenu) {
}
