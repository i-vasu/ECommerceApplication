package com.app.core.multitenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.app.core.multitenancy.Tenant;
import com.app.core.multitenancy.TenantContext;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RazorpayCredentialProvider {

    @Value("${razorpay.test.key.id}")
    private String testKeyId;

    @Value("${razorpay.test.key.secret}")
    private String testKeySecret;

    @Value("${razorpay.test.webhook.secret}")
    private String testWebhookSecret;

    @Value("${razorpay.prod.key.id}")
    private String prodKeyId;

    @Value("${razorpay.prod.key.secret}")
    private String prodKeySecret;

    @Value("${razorpay.prod.webhook.secret}")
    private String prodWebhookSecret;

    public String getKeyId() {
        return isProd() ? prodKeyId : testKeyId;
    }

    public String getKeySecret() {
        return isProd() ? prodKeySecret : testKeySecret;
    }

    public String getWebhookSecret() {
        return isProd() ? prodWebhookSecret : testWebhookSecret;
    }

    private boolean isProd() {
        String tenantId = TenantContext.getTenantId();
        // Default to TEST if no tenant context or if tenant environment is not
        // explicitly PROD
        // In a real scenario, you would look up the Tenant entity here or have it in
        // the context.
        // For simplicity, we'll check if the tenant ID contains "prod" or if we have a
        // way to access the full Tenant object.
        // Since TenantContext only has ID, and we don't want to query DB every time,
        // we might assume a convention or inject a TenantService if needed.
        // However, for now, let's rely on a simple check or defaults.

        // Improved logic: If we can't determine, safer to default to TEST.
        if (tenantId == null)
            return false;

        return tenantId.toLowerCase().contains("prod");
    }

    // Helper to explicity get for a specific Tenant object if available
    public String getKeyId(Tenant tenant) {
        return "PROD".equalsIgnoreCase(tenant.getEnvironment()) ? prodKeyId : testKeyId;
    }

    public String getKeySecret(Tenant tenant) {
        return "PROD".equalsIgnoreCase(tenant.getEnvironment()) ? prodKeySecret : testKeySecret;
    }

    public String getWebhookSecret(Tenant tenant) {
        return "PROD".equalsIgnoreCase(tenant.getEnvironment()) ? prodWebhookSecret : testWebhookSecret;
    }

    public String getTestWebhookSecret() {
        return testWebhookSecret;
    }

    public String getProdWebhookSecret() {
        return prodWebhookSecret;
    }
}
