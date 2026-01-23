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
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.DirectChannel;

@Configuration
public class ERPNextSyncConfig {

    @Bean
    public MessageChannel syncRequestChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow syncFlow(ProductDataFlowService dataFlowService, ImageService imageService) {
        return IntegrationFlow.from("syncRequestChannel")
                .enrichHeaders(h -> h.headerExpression("Authorization",
                        "'token ' + headers['erpNextApiKey'] + ':' + headers['erpNextApiSecret']"))
                .handle(Http.outboundGateway(m -> m.getHeaders().get("erpNextUrl")
                        + "?fields=[\"name\",\"item_name\",\"description\",\"standard_rate\",\"image\",\"item_group\",\"has_variants\",\"variant_of\",\"brand\"]&limit_page_length=100")
                        .httpMethod(HttpMethod.GET)
                        .expectedResponseType(Map.class)
                        .charset("UTF-8"))
                .<Map<String, Object>, List<Map<String, Object>>>transform(this::extractDataList)
                .split() // Split into individual items
                .channel(c -> c.executor(Executors.newVirtualThreadPerTaskExecutor())) // Virtual Threads for parallel
                                                                                       // processing
                .handle((payload, headers) -> {
                    if (payload instanceof Map<?, ?> itemData) {
                        dataFlowService.processItemWithMedia(castToMap(itemData));
                    }
                    return null;
                })
                .get();
    }

    private List<Map<String, Object>> extractDataList(Map<String, Object> payload) {
        if (payload.get("data") instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(this::castToMap)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        return (Map<String, Object>) obj;
    }
}
