package com.app.order.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import com.app.order.services.ERPNextService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderCancellationConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    @Autowired
    private ERPNextService erpNextService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            // EventProducer sends JSON string inside the record value
            String jsonEntry = message.getValue();
            String erpOrderName = objectMapper.readValue(jsonEntry, String.class);
            
            System.out.println("Processing Order Cancellation for: " + erpOrderName);
            erpNextService.cancelSalesOrder(erpOrderName);
            
        } catch (Exception e) {
            System.err.println("Error processing cancellation: " + e.getMessage());
        }
    }
}
