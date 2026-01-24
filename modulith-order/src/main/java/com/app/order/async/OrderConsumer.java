package com.app.order.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.order.entities.Order;
import com.app.order.repositories.OrderRepo;
import com.app.order.services.ERPNextService;

@Component
public class OrderConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    @Autowired
    private ERPNextService erpNextService;

    @Autowired
    private OrderRepo orderRepo;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            String orderIdStr = message.getValue();
            Long orderId = Long.valueOf(orderIdStr);

            System.out.println(">>> Consuming Order Event from DragonflyDB: " + orderId);

            Order order = orderRepo.findById(orderId).orElse(null);
            if (order != null) {
                // Execute Sync Logic
                // Note: We use the existing logic which validates/pushes to ERPNext
                erpNextService.createSalesOrderAsync(order); 
            } else {
                System.err.println("Order not found in DB: " + orderId);
            }
        } catch (Exception e) {
            System.err.println("Error processing order from queue: " + e.getMessage());
        }
    }
}
