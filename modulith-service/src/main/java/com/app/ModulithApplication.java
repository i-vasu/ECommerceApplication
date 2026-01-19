package com.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.modulith.Modulith;

@SpringBootApplication
@EnableScheduling
@org.springframework.retry.annotation.EnableRetry
@org.springframework.modulith.Modulithic
public class ModulithApplication {

    public static void main(String[] args) {
        // Enable Java Vector API SIMD for Visual Search
        System.setProperty("jdk.incubator.vector.VECTOR_ACCESS_STRATEGY", "MAX");

        SpringApplication.run(ModulithApplication.class, args);
    }
}
