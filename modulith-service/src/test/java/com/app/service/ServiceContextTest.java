package com.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import com.app.ModulithApplication;
import com.app.config.RedisStreamConfig;

@SpringBootTest(classes = ModulithApplication.class)
public class ServiceContextTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    RedisStreamConfig redisStreamConfig;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        assertThat(redisStreamConfig).isNotNull();
    }

    @Test
    void redisContainerIsRegistered() {
        assertThat(context.getBean(StreamMessageListenerContainer.class)).isNotNull();
    }
}
