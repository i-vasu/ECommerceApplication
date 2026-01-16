package com.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.MessageHandler;

/**
 * Spring Integration Configuration
 * Enables event-driven architecture with Redis pub/sub
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class IntegrationConfig {

    /**
     * Product events channel
     */
    @Bean
    public ChannelTopic productEventsTopic() {
        return new ChannelTopic("product-events");
    }

    /**
     * Order events channel
     */
    @Bean
    public ChannelTopic orderEventsTopic() {
        return new ChannelTopic("order-events");
    }

    /**
     * Redis message listener container
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * Outbound message handler for publishing events
     */
    @Bean
    public MessageHandler redisPublisher(RedisConnectionFactory connectionFactory) {
        RedisPublishingMessageHandler handler = new RedisPublishingMessageHandler(connectionFactory);
        handler.setDefaultTopic("app-events");
        return handler;
    }

    /**
     * Example integration flow for product events
     */
    @Bean
    public IntegrationFlow productEventFlow(RedisConnectionFactory connectionFactory) {
        return IntegrationFlow
                .from(new RedisInboundChannelAdapter(connectionFactory))
                .channel("productEventsChannel")
                .get();
    }
}
