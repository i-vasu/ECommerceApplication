package com.app.finance.integration.zoho;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zoho.books")
@Data
public class ZohoProperties {
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String organizationId;
    private String baseUrl = "https://www.zohoapis.in/books/v3";
    private String authUrl = "https://accounts.zoho.in/oauth/v2/token";
    private boolean enabled = false;
}
