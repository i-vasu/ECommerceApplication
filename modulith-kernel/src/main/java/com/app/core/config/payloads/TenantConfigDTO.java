package com.app.core.config.payloads;

import lombok.Builder;

import java.util.Map;

@Builder
public record TenantConfigDTO(
        String name,
        String logoUrl,
        String accentColor,
        String supportEmail,
        String heroVideoUrl,
        String heroPosterUrl,
        Map<String, String> socialLinks,
        Map<String, String> staticPages,
        Map<String, Object> footerMenu) {
}
