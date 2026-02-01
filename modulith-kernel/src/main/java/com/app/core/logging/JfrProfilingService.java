package com.app.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JfrProfilingService {
    private static final Logger log = LoggerFactory.getLogger(JfrProfilingService.class);

    public void startRecording(int durationSeconds) {
        log.info("Starting JFR recording for {} seconds", durationSeconds);
        // Implementation omitted
    }
}
