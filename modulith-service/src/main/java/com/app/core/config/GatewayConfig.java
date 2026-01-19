package com.app.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayConfig {

    /**
     * Unified Gateway routing using Spring MVC Router Functions.
     * This replaces the legacy reactive gateway and is optimized for Virtual
     * Threads.
     */
    @Bean
    public RouterFunction<ServerResponse> gatewayRouterFunctions() {
        return route("product_service")
                .route(path("/api/v1/public/products/**"), http())
                .build()
                .and(route("order_service")
                        .route(path("/api/v1/orders/**"), http())
                        .build())
                .and(route("marketing_service")
                        .route(path("/api/v1/marketing/**"), http())
                        .build());
    }
}
