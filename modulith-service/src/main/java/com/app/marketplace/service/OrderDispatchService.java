package com.app.marketplace.service;

import com.app.order.payloads.OrderDTO;
import com.app.marketplace.dto.external.ExternalOrderDTO;
import com.app.marketplace.dto.external.ExternalOrderItemDTO;
import com.app.order.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderDispatchService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderDispatchService.class);

    private final OrderService orderService;

    public void dispatch(OrderDTO orderDTO) {
        log.info("Dispatching order to Core Business Service: {}", orderDTO.getOrderId());

        ExternalOrderDTO externalOrder = mapToExternal(orderDTO);

        try {
            orderService.placeMarketplaceOrder(orderDTO);
            log.info("Order successfully dispatched: {}", orderDTO.getOrderId());
        } catch (Exception e) {
            log.error("Failed to dispatch order: {}", orderDTO.getOrderId(), e);
            throw e; // Rethrow to trigger DLQ or retry
        }
    }

    private ExternalOrderDTO mapToExternal(OrderDTO source) {
        ExternalOrderDTO target = new ExternalOrderDTO();
        target.setEmail(source.getEmail());
        if (source.getTotalAmount() != null) {
            target.setTotalAmount(java.math.BigDecimal.valueOf(source.getTotalAmount()));
        }
        target.setOrderStatus("CREATED"); // Default status

        // Map items
        if (source.getOrderItems() != null) {
            target.setOrderItems(source.getOrderItems().stream().map(item -> {
                ExternalOrderItemDTO extItem = new ExternalOrderItemDTO();
                // We'd need to resolve Product ID from SKU here in a real app
                // For now, we mock or pass 0L
                extItem.setProductId(0L);
                extItem.setQuantity(item.getQuantity());
                extItem.setOrderedProductPrice(item.getOrderedProductPrice());
                return extItem;
            }).collect(Collectors.toList()));
        }

        return target;
    }
}
