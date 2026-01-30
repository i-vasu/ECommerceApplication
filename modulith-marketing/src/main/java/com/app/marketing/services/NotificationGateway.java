package com.app.marketing.services;

import java.util.Map;

public interface NotificationGateway {
    void sendMessage(String recipient, String message);
    void sendTemplate(String recipient, String templateId, Map<String, String> variables);
}
