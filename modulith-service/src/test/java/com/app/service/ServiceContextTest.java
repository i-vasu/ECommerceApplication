package com.app.service;

import com.app.ModulithApplication;
import com.app.config.RedisStreamConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ModulithApplication.class, properties = "spring.autoconfigure.exclude=org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration")
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
        assertThat(context.getBean("streamMessageListenerContainer", StreamMessageListenerContainer.class)).isNotNull();
        assertThat(context.getBean("mapStreamMessageListenerContainer", StreamMessageListenerContainer.class)).isNotNull();
    }
}
