package com.app.marketing.services;

import com.app.catalog.entities.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

// Google Merchant API Imports
import com.google.auth.oauth2.GoogleCredentials;
import com.google.shopping.merchant.products.v1beta.ProductsServiceClient;
import com.google.shopping.merchant.products.v1beta.ProductsServiceSettings;



@Service
@Slf4j
public class GoogleMerchantService {

    @Value("${google.merchant.api.merchant-id:}")
    private String merchantId;

    @Value("${google.merchant.api.service-account-key:}")
    private String serviceAccountKey; 

    public void syncProduct(Product product) {
        if (merchantId == null || merchantId.isEmpty()) {
            log.info("Google Merchant integration disabled (merchant-id missing). Skipping sync for product: {}", product.getItemCode());
            return;
        }

        if (serviceAccountKey == null || serviceAccountKey.isEmpty()) {
            log.warn("Google Service Account Key missing. Cannot authenticate with Google Merchant for product: {}", product.getItemCode());
            return;
        }

        try {
            // Setup Credentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/content"));

            // Initialize Client
            ProductsServiceSettings settings = ProductsServiceSettings.newBuilder()
                .setCredentialsProvider(com.google.api.gax.core.FixedCredentialsProvider.create(credentials))
                .build();

            try (ProductsServiceClient client = ProductsServiceClient.create(settings)) {
                log.info("Google Merchant Client initialized successfully.");

                // TODO: Implement Product Creation.
                // The library com.google.shopping:google-shopping-merchant-products:1.5.0
                // uses v1beta but specific Request/Builder classes need verification against docs.
                // 
                /* 
                // Placeholder for future implementation:
                String parent = "accounts/" + merchantId;
                
                // Product construction...
                // Request construction...
                // client.insertProduct(request); 
                */
                
                log.info("Product sync skipped pending API method verification for product: {}", product.getItemCode());
            }

        } catch (Exception e) {
            log.error("Failed to sync product to Google Merchant", e);
        }
    }
}
