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

    /**
     * Get invoice by order ID
     */
    public Invoice getInvoiceByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(String.valueOf(orderId))
                .orElseThrow(() -> new com.app.core.ResourceNotFoundException("Invoice", "orderId", orderId));
    }

    /**
     * Generate PDF invoice (HTML to PDF conversion using Flying Saucer)
     * Returns PDF as byte array for download
     */
    public byte[] generateInvoicePdf(Long orderId, String requesterEmail, boolean isAdmin) {
        Invoice invoice = getInvoiceByOrderId(orderId);
        
        if (!isAdmin && !invoice.getCustomerEmail().equals(requesterEmail)) {
             throw new com.app.core.APIException("Unauthorized: You do not own this order's invoice.");
        }
        
        // Generate HTML invoice
        String html = generateInvoiceHtml(invoice);
        
        // Convert HTML to PDF using Flying Saucer
        try (java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream()) {
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF invoice for order " + orderId, e);
        }
    }

    /**
     * Generate GST-compliant invoice HTML
     */
    private String generateInvoiceHtml(Invoice invoice) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Tax Invoice - ").append(invoice.getInvoiceNumber()).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append(".invoice-details { margin-bottom: 20px; }");
        html.append(".total { font-weight: bold; font-size: 1.2em; }");
        html.append("</style></head><body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>TAX INVOICE</h1>");
        html.append("<p><strong>Vaabhi Fashion Store</strong></p>");
        html.append("<p>GSTIN: 29AABCU9603R1ZM</p>");
        html.append("</div>");
        
        // Invoice Details
        html.append("<div class='invoice-details'>");
        html.append("<p><strong>Invoice Number:</strong> ").append(invoice.getInvoiceNumber()).append("</p>");
        html.append("<p><strong>Order ID:</strong> ").append(invoice.getOrderId()).append("</p>");
        html.append("<p><strong>Date:</strong> ").append(invoice.getGeneratedAt()).append("</p>");
        html.append("<p><strong>Customer Email:</strong> ").append(invoice.getCustomerEmail()).append("</p>");
        html.append("</div>");
        
        // Items Table
        html.append("<table>");
        html.append("<thead><tr>");
        html.append("<th>Item</th><th>Quantity</th><th>Price</th><th>Tax</th><th>Total</th>");
        html.append("</tr></thead><tbody>");
        
        if (invoice.getItems() != null) {
            for (Map<String, Object> item : invoice.getItems()) {
                html.append("<tr>");
                html.append("<td>").append(item.get("name")).append("</td>");
                html.append("<td>").append(item.get("quantity")).append("</td>");
                html.append("<td>₹").append(item.get("price")).append("</td>");
                html.append("<td>₹").append(item.get("tax")).append("</td>");
                double itemTotal = ((Number) item.get("price")).doubleValue() * ((Number) item.get("quantity")).intValue();
                html.append("<td>₹").append(String.format("%.2f", itemTotal)).append("</td>");
                html.append("</tr>");
            }
        }
        
        html.append("</tbody></table>");
        
        // Tax Breakdown
        if (invoice.getTaxBreakdown() != null && !invoice.getTaxBreakdown().isEmpty()) {
            html.append("<h3>Tax Breakdown</h3>");
            html.append("<table style='width: 50%;'>");
            invoice.getTaxBreakdown().forEach((key, value) -> {
                html.append("<tr><td>").append(key).append("</td>");
                html.append("<td>₹").append(String.format("%.2f", value)).append("</td></tr>");
            });
            html.append("</table>");
        }
        
        // Total
        html.append("<div class='total' style='margin-top: 30px; text-align: right;'>");
        html.append("<p>Total Amount: ₹").append(String.format("%.2f", invoice.getTotalAmount())).append("</p>");
        html.append("</div>");
        
        html.append("<div style='margin-top: 50px; text-align: center; font-size: 0.9em; color: #666;'>");
        html.append("<p>This is a computer-generated invoice and does not require a signature.</p>");
        html.append("</div>");
        
        html.append("</body></html>");
        return html.toString();
    }
}
