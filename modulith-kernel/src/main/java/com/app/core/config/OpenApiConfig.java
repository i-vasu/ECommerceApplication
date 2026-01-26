package com.app.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Value("${server.port:8080}")
        private String serverPort;

        @Bean
        @Primary
        public OpenAPI modulithOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Vaabhi E-Commerce API")
                                                .description("Unified Modular Monolith API Platform")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("Vaabhi Dev Team")
                                                                .email("dev@vaabhi.com"))
                                                .license(new License()
                                                                .name("Proprietary")))
                                .servers(List.of(
                                                new Server()
                                                                .url("http://localhost:" + serverPort)
                                                                .description("Local Modulith Server")))
                                .components(new Components()
                                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        }

        @Bean
        public org.springdoc.core.models.GroupedOpenApi productApi() {
                return org.springdoc.core.models.GroupedOpenApi.builder()
                                .group("Product & Catalog")
                                .packagesToScan("com.app.product", "com.app.search", "com.app.inventory")
                                .build();
        }

        @Bean
        public org.springdoc.core.models.GroupedOpenApi orderApi() {
                return org.springdoc.core.models.GroupedOpenApi.builder()
                                .group("Order & Checkout")
                                .packagesToScan("com.app.order", "com.app.cart", "com.app.commerce", "com.app.payment",
                                                "com.app.shipping")
                                .build();
        }

        @Bean
        public org.springdoc.core.models.GroupedOpenApi identityApi() {
                return org.springdoc.core.models.GroupedOpenApi.builder()
                                .group("Identity & Security")
                                .packagesToScan("com.app.identity", "com.app.core")
                                .build();
        }

        @Bean
        public org.springdoc.core.models.GroupedOpenApi adminApi() {
                return org.springdoc.core.models.GroupedOpenApi.builder()
                                .group("Admin & Management")
                                .packagesToScan("com.app.admin", "com.app.media")
                                .build();
        }

        @Bean
        public org.springdoc.core.models.GroupedOpenApi marketplaceApi() {
                return org.springdoc.core.models.GroupedOpenApi.builder()
                                .group("Marketplace")
                                .packagesToScan("com.app.marketplace")
                                .build();
        }

        @Bean
        public org.springdoc.core.models.GroupedOpenApi engagementApi() {
                return org.springdoc.core.models.GroupedOpenApi.builder()
                                .group("Customer Engagement")
                                .packagesToScan("com.app.marketing", "com.app.notification", "com.app.review",
                                                "com.app.discount", "com.app.customer_service")
                                .build();
        }
}
