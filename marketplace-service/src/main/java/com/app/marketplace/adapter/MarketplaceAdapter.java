package com.app.marketplace.adapter;

import com.app.marketplace.dto.OrderDTO;
import java.util.List;
import java.util.Map;

public interface MarketplaceAdapter {
    List<OrderDTO> fetchOrders();

    void handleWebhook(Map<String, Object> payload);

    OrderDTO normalize(Object payload);
}
