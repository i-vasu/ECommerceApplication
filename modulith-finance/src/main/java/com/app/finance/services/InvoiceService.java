package com.app.finance.services;

import com.app.core.contracts.OrderAmountProvider;
import com.app.core.events.OrderPaidEvent;
import com.app.finance.entities.Invoice;
import com.app.finance.repositories.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderAmountProvider orderProvider;

    @Async
    @EventListener
    @Transactional
    public void generateInvoice(OrderPaidEvent event) {
        log.info("Generating Invoice for Order ID: {}", event.orderId());
         String orderIdStr = String.valueOf(event.orderId());

        if (invoiceRepository.findByOrderId(orderIdStr).isPresent()) {
            log.warn("Invoice already exists for Order {}", event.orderId());
            return;
        }

        try {
            OrderAmountProvider.OrderSummary order = orderProvider.getOrderSummary(event.orderId())
                    .orElseThrow(() -> new RuntimeException("Order details not found for " + event.orderId()));

            Invoice invoice = new Invoice();
            invoice.setOrderId(orderIdStr);
            invoice.setCustomerEmail(order.email());
            
            // Format: INV-{YYYY}-{ORDER_ID}
            invoice.setInvoiceNumber("INV-" + java.time.Year.now().getValue() + "-" + order.orderId());
            
            invoice.setTotalAmount(order.totalAmount().doubleValue());
            
            if (order.taxAmount() != null) {
                invoice.setTaxAmount(order.taxAmount().doubleValue());
            }

            // Map Tax Breakdown (BigDecimal -> Double)
            if (order.taxBreakdown() != null) {
                Map<String, Double> taxes = new HashMap<>();
                order.taxBreakdown().forEach((k, v) -> taxes.put(k, v.doubleValue()));
                invoice.setTaxBreakdown(taxes);
            }

            // Snapshot Items
            if (order.items() != null) {
                List<Map<String, Object>> itemSnapshots = order.items().stream()
                    .map(item -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("itemCode", item.itemCode());
                        map.put("name", item.name());
                        map.put("quantity", item.quantity());
                        map.put("price", item.price());
                        map.put("tax", item.taxAmount());
                        return map;
                    })
                    .collect(Collectors.toList());
                invoice.setItems(itemSnapshots);
            }

            invoice.setStatus(Invoice.InvoiceStatus.GENERATED);
            
            invoiceRepository.save(invoice);
            log.info("Invoice {} generated successfully.", invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("Failed to generate invoice for Order {}: {}", event.orderId(), e.getMessage(), e);
        }
    }
}
