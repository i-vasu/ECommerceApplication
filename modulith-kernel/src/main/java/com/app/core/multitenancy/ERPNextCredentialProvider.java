package com.app.core.multitenancy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ERPNextCredentialProvider {

    @Autowired
    private TenantRepository tenantRepository;

    @Value("${erpnext.api.base-url:}")
    private String defaultUrl;

    @Value("${erpnext.api.key:}")
    private String defaultKey;

    @Value("${erpnext.api.secret:}")
    private String defaultSecret;

    public String getBaseUrl() {
        return getTenant().map(Tenant::getErpNextUrl)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse(defaultUrl);
    }

    public String getApiKey() {
        return getTenant().map(Tenant::getErpNextApiKey)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse(defaultKey);
    }

    public String getApiSecret() {
        return getTenant().map(Tenant::getErpNextApiSecret)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse(defaultSecret);
    }

    public String getCompanyName() {
        return getTenant().map(Tenant::getErpNextCompanyName)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse("Default Company");
    }

    public String getWarehouse() {
        return getTenant().map(Tenant::getErpNextWarehouse)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse("Stores - Default");
    }

    public String getWebhookSecret() {
        return getTenant().map(Tenant::getErpNextWebhookSecret)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse(null);
    }

    private java.util.Optional<Tenant> getTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || "public".equals(tenantId)) {
            return java.util.Optional.empty();
        }
        return tenantRepository.findByTenantId(tenantId);
    }
}
