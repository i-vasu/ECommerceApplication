package com.app.erp_sync.gateway;


// import com.app.media.ImageService;
// import com.app.search.services.ProductDataFlowService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
public class ERPNextSyncConfig {

    @Bean
    public static MessageChannel syncRequestChannel() {
        return new DirectChannel();
    }

    @Bean
    public static IntegrationFlow syncFlow(org.springframework.context.ApplicationEventPublisher eventPublisher) {
        org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer producer = 
            new org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer();
        producer.setEventTypes(com.app.core.events.ERPProductSyncRequestedEvent.class);

        return IntegrationFlow.from(producer)
                .enrichHeaders(h -> h
                        .headerExpression("erpNextApiKey", "payload.erpNextApiKey()")
                        .headerExpression("erpNextApiSecret", "payload.erpNextApiSecret()")
                        .headerExpression("erpNextUrl", "payload.erpNextUrl()")
                        .headerExpression("Authorization",
                                "'token ' + payload.erpNextApiKey() + ':' + payload.erpNextApiSecret()"))
                .handle(Http.outboundGateway(m -> m.getHeaders().get("erpNextUrl")
                        + "/api/resource/Item?fields=[\"name\",\"item_name\",\"description\",\"standard_rate\",\"image\",\"item_group\",\"has_variants\",\"variant_of\",\"brand\"]&limit_page_length=100")
                        .httpMethod(HttpMethod.GET)
                        .expectedResponseType(Map.class)
                        .charset("UTF-8"))
                .<Map<String, Object>, List<Map<String, Object>>>transform(ERPNextSyncConfig::extractDataList)
                .split() // Split into individual items
                .channel(c -> c.executor(Executors.newVirtualThreadPerTaskExecutor())) // Virtual Threads for parallel processing
                .handle((payload, headers) -> {
                    if (payload instanceof Map<?, ?> itemData) {
                        // DECOUPLED: Publish event for each item
                        eventPublisher.publishEvent(new com.app.core.events.ERPItemSyncRequestedEvent(ERPNextSyncConfig.castToMap(itemData)));
                    }
                    return null;
                })
                .get();
    }

    private static List<Map<String, Object>> extractDataList(Map<String, Object> payload) {
        if (payload.get("data") instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(ERPNextSyncConfig::castToMap)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMap(Object obj) {
        return (Map<String, Object>) obj;
    }
}
