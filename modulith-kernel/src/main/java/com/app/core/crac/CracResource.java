package com.app.core.crac;

import jakarta.annotation.PostConstruct;
import org.crac.Context;
import org.crac.Resource;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CracResource implements Resource {

    private static final Logger log = LoggerFactory.getLogger(CracResource.class);

    @PostConstruct
    public void init() {
        org.crac.Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(@NonNull Context<? extends Resource> context) throws Exception {
        log.info(">>> CRaC: Preparing for checkpoint. Closing resources...");
        // Close DB connections, file handles, or network sockets if necessary.
        // Spring Boot 3.2+ / Spring 6.1+ handle many of these automatically.
    }

    @Override
    public void afterRestore(@NonNull Context<? extends Resource> context) throws Exception {
        log.info(">>> CRaC: Restore complete. Application is warm and ready!");
        // Re-open or refresh resources if needed.
    }
}
