package com.app.marketplace.service;

import com.app.order.order.OrderService;
import com.app.order.payloads.OrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderDispatchService {

    private final OrderService orderService;

    public void dispatch(OrderDTO orderDTO) {
        orderService.placeMarketplaceOrder(orderDTO);
    }
}
