package com.app.erp_sync.gateway;

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
