package com.app.product.integration;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;

@MessagingGateway
public interface SyncGateway {

    @Gateway(requestChannel = "syncRequestChannel")
    void startSync(String trigger,
            @Header("erpNextUrl") String url,
            @Header("erpNextApiKey") String apiKey,
            @Header("erpNextApiSecret") String apiSecret);
}
