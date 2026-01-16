package com.app.integration;

import com.app.services.ImageService;
import com.app.search.ProductDataFlowService;
import com.app.payloads.ProductMediaDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.Message;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Configuration
@EnableIntegration
@IntegrationComponentScan
public class ERPNextSyncConfig {

    @Value("${erpnext.api.url}")
    private String erpNextUrl;

    @Value("${erpnext.api.key}")
    private String apiKey;

    @Value("${erpnext.api.secret}")
    private String apiSecret;

    @Bean
    public IntegrationFlow syncFlow(ProductDataFlowService dataFlowService, ImageService imageService) {
        return IntegrationFlow.from("syncRequestChannel")
                .handle(Http.outboundGateway(erpNextUrl
                        + "?fields=[\"name\",\"item_name\",\"description\",\"standard_rate\",\"image\",\"item_group\",\"has_variants\",\"variant_of\"]&limit_page_length=100")
                        .httpMethod(HttpMethod.GET)
                        .expectedResponseType(Map.class)
                        .charset("UTF-8"))
                .<Map<String, Object>, List<Map<String, Object>>>transform(
                        payload -> (List<Map<String, Object>>) payload.get("data"))
                .split() // Split into individual items
                .channel(c -> c.executor(Executors.newVirtualThreadPerTaskExecutor())) // Virtual Threads for parallel
                                                                                       // processing
                .handle((payload, headers) -> {
                    Map<String, Object> itemData = (Map<String, Object>) payload;
                    // Process item and its media
                    dataFlowService.processItemWithMedia(itemData);
                    return null;
                })
                .get();
    }
}
