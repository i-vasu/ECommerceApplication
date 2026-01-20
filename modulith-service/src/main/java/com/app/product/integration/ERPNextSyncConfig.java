package com.app.product.integration;

import com.app.media.ImageService;
import com.app.search.services.ProductDataFlowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.http.dsl.Http;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
public class ERPNextSyncConfig {

    @Bean
    public IntegrationFlow syncFlow(ProductDataFlowService dataFlowService, ImageService imageService) {
        return IntegrationFlow.from("syncRequestChannel")
                .enrichHeaders(h -> h.headerExpression("Authorization",
                        "'token ' + headers['erpNextApiKey'] + ':' + headers['erpNextApiSecret']"))
                .handle(Http.outboundGateway(m -> 
                        m.getHeaders().get("erpNextUrl") + "?fields=[\"name\",\"item_name\",\"description\",\"standard_rate\",\"image\",\"item_group\",\"has_variants\",\"variant_of\",\"brand\"]&limit_page_length=100")
                        .httpMethod(HttpMethod.GET)
                        .expectedResponseType(Map.class)
                        .charset("UTF-8"))
                .<Map<String, Object>, List<Map<String, Object>>>transform(
                        payload -> {
                            if (payload.get("data") instanceof List<?> list) {
                                return (List<Map<String, Object>>) list;
                            }
                            return List.of();
                        })
                .split() // Split into individual items
                .channel(c -> c.executor(Executors.newVirtualThreadPerTaskExecutor())) // Virtual Threads for parallel
                                                                                       // processing
                .handle((payload, headers) -> {
                    if (payload instanceof Map<?, ?> itemData) {
                        // Process item and its media
                        dataFlowService.processItemWithMedia((Map<String, Object>) itemData);
                    }
                    return null;
                })
                .get();
    }
}
