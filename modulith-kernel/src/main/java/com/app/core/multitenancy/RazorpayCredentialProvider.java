package com.app.core.multitenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RazorpayCredentialProvider {

    @org.springframework.beans.factory.annotation.Autowired
    private TenantRepository tenantRepository;

    @org.springframework.beans.factory.annotation.Value("${razorpay.test.key.id:}")
    private String defaultKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.test.key.secret:}")
    private String defaultKeySecret;

    @org.springframework.beans.factory.annotation.Value("${razorpay.test.webhook.secret:}")
    private String defaultWebhookSecret;

    public String getKeyId() {
        return getTenant().map(Tenant::getRazorpayKeyId)
                .filter(s -> s != null && !s.isBlank())
                .orElse(defaultKeyId);
    }

    public String getKeySecret() {
        return getTenant().map(Tenant::getRazorpayKeySecret)
                .filter(s -> s != null && !s.isBlank())
                .orElse(defaultKeySecret);
    }

    public String getWebhookSecret() {
        return getTenant().map(Tenant::getRazorpayWebhookSecret)
                .filter(s -> s != null && !s.isBlank())
                .orElse(defaultWebhookSecret);
    }

    private java.util.Optional<Tenant> getTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || "public".equals(tenantId)) {
            return java.util.Optional.empty();
        }
        return tenantRepository.findByTenantId(tenantId);
    }
}
