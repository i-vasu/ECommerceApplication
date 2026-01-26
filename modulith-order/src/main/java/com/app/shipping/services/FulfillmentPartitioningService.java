package com.app.shipping.services;

import com.app.order.entities.Order;
import com.app.order.entities.OrderItem;
import com.app.order.entities.FulfillmentGroup;
import com.app.order.repositories.FulfillmentGroupRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enterprise Fulfillment Partitioning Service.
 * Implements the 'Split Order' logic from Broadleaf.
 * Groups items by warehouse or shipping requirements.
 */
@Service
@RequiredArgsConstructor
public class FulfillmentPartitioningService {

    private final FulfillmentGroupRepo fulfillmentGroupRepo;

    public List<FulfillmentGroup> partitionOrder(Order order) {
        // Broadleaf logic: group by warehouse location
        // ForPoC: We simulate that products with itemCodes starting with 'MUM-'
        // come from Mumbai, others from 'BLR-' (Bangalore)

        Map<String, List<OrderItem>> partitions = order.getOrderItems().stream()
                .collect(Collectors.groupingBy(item -> {
                    if (item.getItemCode().startsWith("MUM"))
                        return "MUMBAI";
                    return "BANGALORE";
                }));

        List<FulfillmentGroup> groups = new ArrayList<>();

        partitions.forEach((location, items) -> {
            FulfillmentGroup group = new FulfillmentGroup();
            group.setOrder(order);
            group.setAddressState(order.getFulfillmentGroups().isEmpty() ? "Default" : "Custom");
            group.setItems(items);
            // In a real system, we would calculate shipping cost per group here
            group.setShippingCost(100.0);
            groups.add(group);
        });

        return fulfillmentGroupRepo.saveAll(groups);
    }
}
