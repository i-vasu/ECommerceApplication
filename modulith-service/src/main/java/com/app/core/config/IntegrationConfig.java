package com.app.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.messaging.MessageHandler;

@Configuration
@EnableIntegration
@IntegrationComponentScan(basePackages = "com.app")
public class IntegrationConfig {

    @Bean
    public ChannelTopic productEventsTopic() {
        return new ChannelTopic("product-events");
    }

    @Bean
    public ChannelTopic orderEventsTopic() {
        return new ChannelTopic("order-events");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public MessageHandler redisPublisher(RedisConnectionFactory connectionFactory) {
        RedisPublishingMessageHandler handler = new RedisPublishingMessageHandler(connectionFactory);
        handler.setTopic("app-events");
        return handler;
    }

    @Bean
    public IntegrationFlow productEventFlow(RedisConnectionFactory connectionFactory) {
        RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(connectionFactory);
        adapter.setTopics("product-events");
        return IntegrationFlow
                .from(adapter)
                .channel("productEventsChannel")
                .get();
    }
}
