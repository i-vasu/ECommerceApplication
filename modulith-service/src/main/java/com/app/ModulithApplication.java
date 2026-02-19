package com.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.modulith.Modulithic;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {"com.app", "org.springframework.statemachine.data.jpa"},
    exclude = {org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration.class}
)
@EnableScheduling
@EnableRetry
@Modulithic
@EnableAsync
@EnableJpaRepositories(basePackages = {"com.app", "org.springframework.statemachine.data.jpa", "org.springframework.modulith.events.jpa"}, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "(com\\.app\\.support\\.repositories\\.jdbc\\..*|com\\.app\\.discovery\\.domain\\.repositories\\.jdbc\\..*)"))
@org.springframework.data.jdbc.repository.config.EnableJdbcRepositories(basePackages = {"com.app.support.repositories.jdbc", "com.app.discovery.domain.repositories.jdbc"})
@EntityScan(basePackages = {"com.app", "org.springframework.statemachine.data.jpa", "org.springframework.modulith.events.jpa"})
public class ModulithApplication {

    public static void main(String[] args) {
        // Enable Java Vector API SIMD for Visual Search
        System.setProperty("jdk.incubator.vector.VECTOR_ACCESS_STRATEGY", "MAX");

        SpringApplication.run(ModulithApplication.class, args);
    }
}
