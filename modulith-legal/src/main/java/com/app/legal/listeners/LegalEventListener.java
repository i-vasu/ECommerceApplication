package com.app.legal.listeners;

import com.app.core.events.UserRegisteredEvent;
import com.app.legal.services.LegalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegalEventListener {

    private final LegalService legalService;

    @Async
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Recording default legal acceptance for new user: {}", event.email());
        try {
            // Automatically record acceptance of Terms of Service during registration
            legalService.acceptAgreement(event.email(), "TERMS_OF_SERVICE", "0.0.0.0", "SYSTEM_SIGNUP");
        } catch (Exception e) {
            log.warn("Could not record automatic legal acceptance for {}: {}", event.email(), e.getMessage());
        }
    }
}
