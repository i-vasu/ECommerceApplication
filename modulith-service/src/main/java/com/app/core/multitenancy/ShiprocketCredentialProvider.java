package com.app.core.multitenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ShiprocketCredentialProvider {

    @Value("${shiprocket.test.email}")
    private String testEmail;

    @Value("${shiprocket.test.password}")
    private String testPassword;

    @Value("${shiprocket.test.channel.id}")
    private String testChannelId;

    @Value("${shiprocket.prod.email}")
    private String prodEmail;

    @Value("${shiprocket.prod.password}")
    private String prodPassword;

    @Value("${shiprocket.prod.channel.id}")
    private String prodChannelId;

    public String getEmail(Tenant tenant) {
        return isProd(tenant) ? prodEmail : testEmail;
    }

    public String getPassword(Tenant tenant) {
        return isProd(tenant) ? prodPassword : testPassword;
    }

    public String getChannelId(Tenant tenant) {
        return isProd(tenant) ? prodChannelId : testChannelId;
    }

    // Fallback using TenantContext if Tenant object not passed directly
    public String getEmail() {
        return isProd() ? prodEmail : testEmail;
    }

    public String getPassword() {
        return isProd() ? prodPassword : testPassword;
    }

    public String getChannelId() {
        return isProd() ? prodChannelId : testChannelId;
    }

    private boolean isProd(Tenant tenant) {
        return tenant != null && "PROD".equalsIgnoreCase(tenant.getEnvironment());
    }

    private boolean isProd() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && tenantId.toLowerCase().contains("prod");
    }
}
