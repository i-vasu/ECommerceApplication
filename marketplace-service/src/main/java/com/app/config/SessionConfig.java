package com.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Spring Session Configuration
 * Enables distributed session management using Dragonfly/Redis
 * Sessions automatically stored in Redis with 30-minute timeout
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes
public class SessionConfig {
    // Sessions automatically managed by Spring Session
    // No additional configuration needed - uses existing RedisConnectionFactory
}
