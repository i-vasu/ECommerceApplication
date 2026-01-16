package com.app.integration;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
public interface SyncGateway {

    @Gateway(requestChannel = "syncRequestChannel")
    void startSync(String trigger);
}
