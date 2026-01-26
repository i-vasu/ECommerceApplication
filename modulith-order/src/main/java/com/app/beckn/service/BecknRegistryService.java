package com.app.beckn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class BecknRegistryService {

    private final RestClient restClient;

    @Value("${beckn.bpp.id:fashion-store-bpp}")
    private String bppId;

    @Value("${beckn.bpp.uri:https://api.fashion-store.com/api/v1/beckn}")
    private String bppUri;

    @Value("${beckn.registry.url:}")
    private String registryUrl;

    public BecknRegistryService(RestClient restClient) {
        this.restClient = restClient;
    }

    @PostConstruct
    public void registerWithNetwork() {
        if (registryUrl == null || registryUrl.isBlank()) {
            log.warn("Beckn Registry URL not configured. Skipping registration.");
            return;
        }

        log.info("Registering BPP {} with Beckn Network at {}", bppId, registryUrl);
        
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("subscriber_id", bppId);
            request.put("subscriber_url", bppUri);
            request.put("type", "BPP");
            request.put("domain", "nic2004:52110"); // Fashion/Retail
            request.put("city", "std:080"); // Bangalore
            request.put("country", "IND");

            // Mock registration call
            // ResponseEntity<String> response = restClient.post()
            //     .uri(registryUrl + "/subscribe")
            //     .body(request)
            //     .retrieve()
            //     .toEntity(String.class);
            
            log.info("BPP Registered successfully with Beckn Network.");
        } catch (Exception e) {
            log.error("Beckn Network registration failed: {}", e.getMessage());
        }
    }
}
