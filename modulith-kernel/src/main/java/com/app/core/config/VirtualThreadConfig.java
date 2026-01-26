package com.app.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Java 25 Virtual Threads Configuration
 * Centralized for the entire Modulith
 */
@Configuration
@EnableAsync
public class VirtualThreadConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        return new VirtualThreadTaskExecutor("app-async-");
    }

    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
