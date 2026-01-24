package com.app.marketplace.service;

import com.app.order.payloads.OrderDTO;
import com.app.order.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@lombok.extern.log4j.Log4j2
public class OrderDispatchService {

    private final OrderService orderService;

    public void dispatch(OrderDTO orderDTO) {
        log.info("Dispatching order to Core Business Service: {}", orderDTO.orderId());

        try {
            orderService.placeMarketplaceOrder(orderDTO);
            log.info("Order successfully dispatched: {}", orderDTO.orderId());
        } catch (Exception e) {
            log.error("Failed to dispatch order: {}", orderDTO.orderId(), e);
            throw e; // Rethrow to trigger DLQ or retry
        }
    }
}
