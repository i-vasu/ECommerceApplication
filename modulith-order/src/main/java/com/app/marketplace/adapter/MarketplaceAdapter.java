package com.app.marketplace.adapter;

import com.app.order.payloads.OrderDTO;
import java.util.List;
import java.util.Map;

public interface MarketplaceAdapter {
    List<OrderDTO> fetchOrders();

    OrderDTO handleWebhook(Map<String, Object> payload);

    OrderDTO normalize(Object payload);

    void updateInventory(String sku, int quantity);
}
