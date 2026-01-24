package com.app.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Java 25 Optimized HTTP Client Configuration
 * Centralized for the entire Modulith
 * Uses Virtual Threads for non-blocking HTTP calls
 */
@Configuration
public class HttpClientConfig {

    /**
     * Primary RestClient with Virtual Thread executor
     * All HTTP calls will use lightweight virtual threads
     */
    @Bean
    @Primary
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Dedicated RestClient for ERPNext API calls
     * Longer timeout for sync operations
     */
    @Bean
    public RestClient erpNextRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMinutes(5)); // Long timeout for large syncs

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Dedicated RestClient for Payment Gateway (Razorpay)
     * Critical path - optimized for low latency
     */
    @Bean
    public RestClient paymentRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
