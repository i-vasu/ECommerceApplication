package com.app.core.multitenancy;

import com.app.core.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ERPNextCredentialProvider {

    @Autowired
    private TenantRepository tenantRepository;

    @Value("${erpnext.api.base-url:http://localhost:8000}")
    private String defaultUrl;

    @Value("${erpnext.api.key:mock_key}")
    private String defaultKey;

    @Value("${erpnext.api.secret:mock_secret}")
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

    private java.util.Optional<Tenant> getTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || "public".equals(tenantId)) {
            return java.util.Optional.empty();
        }
        return tenantRepository.findByTenantId(tenantId);
    }
}
