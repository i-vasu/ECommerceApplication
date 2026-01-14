package com.app.marketplace.service;

import com.app.marketplace.dto.OrderDTO;
import com.app.marketplace.dto.external.ExternalOrderDTO;
import com.app.marketplace.dto.external.ExternalOrderItemDTO;
import com.app.marketplace.feign.OrderServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDispatchService {

    private final OrderServiceClient orderServiceClient;

    public void dispatch(OrderDTO orderDTO) {
        log.info("Dispatching order to Core Business Service: {}", orderDTO.getMarketplaceOrderId());

        ExternalOrderDTO externalOrder = mapToExternal(orderDTO);

        try {
            orderServiceClient.ingestOrder(externalOrder);
            log.info("Order successfully dispatched: {}", orderDTO.getMarketplaceOrderId());
        } catch (Exception e) {
            log.error("Failed to dispatch order: {}", orderDTO.getMarketplaceOrderId(), e);
            throw e; // Rethrow to trigger DLQ or retry
        }
    }

    private ExternalOrderDTO mapToExternal(OrderDTO source) {
        ExternalOrderDTO target = new ExternalOrderDTO();
        target.setEmail(source.getCustomerEmail());
        target.setTotalAmount(source.getTotalAmount());
        target.setOrderStatus("CREATED"); // Default status

        // Map items
        if (source.getItems() != null) {
            target.setOrderItems(source.getItems().stream().map(item -> {
                ExternalOrderItemDTO extItem = new ExternalOrderItemDTO();
                // We'd need to resolve Product ID from SKU here in a real app
                // For now, we mock or pass 0L
                extItem.setProductId(0L);
                extItem.setQuantity(item.getQuantity());
                extItem.setOrderedProductPrice(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
                return extItem;
            }).collect(Collectors.toList()));
        }

        return target;
    }
}
