package com.app.finance.integration.zoho;

import com.app.core.events.*;
import com.app.finance.entities.ZohoEntityMapping;
import com.app.finance.entities.ZohoEntityMapping.ZohoEntityType;
import com.app.finance.repositories.ZohoEntityMappingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoSyncService {

    private final ZohoClient zohoClient;
    private final ZohoEntityMappingRepository zohoMappingRepo;

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Syncing new product to Zoho: {}", event.productName());
        try {
            Map<String, Object> item = new HashMap<>();
            item.put("name", event.productName());
            item.put("rate", event.price());
            item.put("sku", event.itemCode());
            item.put("description", event.productName());
            item.put("product_type", "goods");
            item.put("is_taxable", true);
            
            zohoClient.createItem(item);
        } catch (Exception e) {
            log.error("Failed to sync product to Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Creating Sales Order in Zoho: {}", event.orderId());
        try {
            // Use email prefix as customer name (best available from this event)
            String customerName = event.email().contains("@") 
                ? event.email().substring(0, event.email().indexOf('@')) : event.email();
            String customerId = zohoClient.findOrCreateCustomer(customerName, event.email());

            List<Map<String, Object>> lineItems = event.items().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", item.productName() != null ? item.productName() : item.itemCode());
                    map.put("rate", item.price());
                    map.put("quantity", item.quantity());
                    return map;
                }).collect(Collectors.toList());

            Map<String, Object> salesOrder = new HashMap<>();
            salesOrder.put("customer_id", customerId);
            salesOrder.put("line_items", lineItems);
            salesOrder.put("date", event.createdAt().toLocalDate().toString());
            salesOrder.put("reference_number", "ORD-" + event.orderId());
            salesOrder.put("salesorder_number", "SO-" + event.orderId());

            String soId = zohoClient.createSalesOrder(salesOrder);

            // Persist Zoho Sales Order ID for bidirectional operations
            zohoMappingRepo.save(ZohoEntityMapping.create(
                ZohoEntityType.SALES_ORDER, String.valueOf(event.orderId()), soId));

            log.info("Sales Order created and mapped in Zoho: {} for Order: {}", soId, event.orderId());

        } catch (Exception e) {
            log.error("Failed to sync sales order to Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Transactional
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Converting Sales Order to Invoice in Zoho: {}", event.getOrderId());
        try {
            String customerId = zohoClient.findOrCreateCustomer(event.getCustomerName(), event.getCustomerEmail());

            List<Map<String, Object>> lineItems = event.getItems().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", item.getProductName());
                    map.put("rate", item.getPrice());
                    map.put("quantity", item.getQuantity());
                    return map;
                }).collect(Collectors.toList());

            // Try to convert existing Sales Order to Invoice first
            Optional<ZohoEntityMapping> soMapping = zohoMappingRepo.findByEntityTypeAndLocalId(
                ZohoEntityType.SALES_ORDER, String.valueOf(event.getOrderId()));

            String zohoInvoiceId = null;
            if (soMapping.isPresent()) {
                try {
                    zohoInvoiceId = zohoClient.convertSalesOrderToInvoice(soMapping.get().getZohoId());
                    log.info("Converted Zoho SO {} to Invoice {}", soMapping.get().getZohoId(), zohoInvoiceId);
                } catch (Exception convEx) {
                    log.warn("Failed to convert SO to Invoice, falling back to direct creation", convEx);
                }
            }

            // Fallback: create invoice directly if conversion failed or no SO exists
            if (zohoInvoiceId == null) {
                Map<String, Object> invoice = new HashMap<>();
                invoice.put("customer_id", customerId);
                invoice.put("line_items", lineItems);
                invoice.put("date", event.getConfirmedAt().toLocalDate().toString());
                invoice.put("due_date", event.getConfirmedAt().toLocalDate().toString());
                invoice.put("reference_number", "ORD-" + event.getOrderId());
                invoice.put("status", "sent");
                zohoInvoiceId = zohoClient.createInvoice(invoice);
                log.info("Invoice created directly in Zoho for Order: {} -> {}", event.getOrderId(), zohoInvoiceId);
            }

            // Persist Zoho Invoice ID for payment linking
            if (zohoInvoiceId != null) {
                zohoMappingRepo.save(ZohoEntityMapping.create(
                    ZohoEntityType.INVOICE, String.valueOf(event.getOrderId()), zohoInvoiceId));
            }

        } catch (Exception e) {
            log.error("Failed to sync invoice to Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Recording customer payment in Zoho for Order: {}", event.orderId());
        try {
            // Derive customer name from email prefix
            String customerName = event.email().contains("@")
                ? event.email().substring(0, event.email().indexOf('@')) : event.email();
            String customerId = zohoClient.findOrCreateCustomer(customerName, event.email());

            Map<String, Object> payment = new HashMap<>();
            payment.put("customer_id", customerId);
            payment.put("payment_mode", event.paymentMethod());
            payment.put("amount", event.amount().doubleValue());
            payment.put("date", java.time.LocalDate.now().toString());
            payment.put("reference_number", event.pgPaymentId());
            payment.put("description", "Payment for Order #" + event.orderId());

            // Link payment to Zoho Invoice if we have the mapping
            Optional<ZohoEntityMapping> invoiceMapping = zohoMappingRepo.findByEntityTypeAndLocalId(
                ZohoEntityType.INVOICE, String.valueOf(event.orderId()));
            if (invoiceMapping.isPresent()) {
                payment.put("invoices", List.of(Map.of(
                    "invoice_id", invoiceMapping.get().getZohoId(),
                    "amount_applied", event.amount().doubleValue()
                )));
                log.info("Payment linked to Zoho Invoice: {}", invoiceMapping.get().getZohoId());
            } else {
                log.warn("No Zoho Invoice mapping found for Order {}. Payment recorded unlinked.", event.orderId());
            }

            zohoClient.recordCustomerPayment(payment);

        } catch (Exception e) {
            log.error("Failed to record payment in Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Transactional
    public void onPurchaseOrderCreated(PurchaseOrderCreatedEvent event) {
        log.info("Syncing Purchase Order to Zoho: {}", event.poId());
        try {
            String vendorId = zohoClient.findOrCreateVendor(event.supplierName());
            
            List<Map<String, Object>> lineItems = event.items().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", "Item-" + item.itemCode());
                    map.put("rate", item.unitPrice());
                    map.put("quantity", item.quantity());
                    return map;
                }).collect(Collectors.toList());

            Map<String, Object> po = new HashMap<>();
            po.put("vendor_id", vendorId);
            po.put("line_items", lineItems);
            po.put("purchaseorder_number", "PO-" + event.poId());
            po.put("date", event.createdAt().toLocalDate().toString());
            po.put("delivery_date", event.expectedDeliveryDate().toString());
            po.put("reference_number", "PO-" + event.poId());

            String zohoPOId = zohoClient.createPurchaseOrder(po);

            // Persist Zoho PO ID for marking as received later
            zohoMappingRepo.save(ZohoEntityMapping.create(
                ZohoEntityType.PURCHASE_ORDER, String.valueOf(event.poId()), zohoPOId));

            log.info("Purchase Order created and mapped in Zoho: {}", zohoPOId);

        } catch (Exception e) {
            log.error("Failed to sync PO to Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onGoodsReceived(GoodsReceivedEvent event) {
        log.info("Syncing PO to Zoho Bill: {}", event.poId());
        try {
            String vendorId = zohoClient.findOrCreateVendor(event.supplierName());
            
            List<Map<String, Object>> lineItems = event.items().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", "Item-" + item.itemCode());
                    map.put("rate", item.unitPrice());
                    map.put("quantity", item.quantity());
                    return map;
                }).collect(Collectors.toList());

            Map<String, Object> bill = new HashMap<>();
            bill.put("vendor_id", vendorId);
            bill.put("line_items", lineItems);
            bill.put("bill_number", "PO-" + event.poId());
            bill.put("date", event.receivedAt().toLocalDate().toString());
            bill.put("due_date", event.receivedAt().plusDays(30).toLocalDate().toString());

            zohoClient.createBill(bill);

            // Mark PO as received in Zoho using stored mapping
            Optional<ZohoEntityMapping> poMapping = zohoMappingRepo.findByEntityTypeAndLocalId(
                ZohoEntityType.PURCHASE_ORDER, String.valueOf(event.poId()));
            if (poMapping.isPresent()) {
                zohoClient.markPurchaseOrderReceived(poMapping.get().getZohoId());
                log.info("PO {} marked as received in Zoho", poMapping.get().getZohoId());
            } else {
                log.warn("No Zoho PO mapping found for PO {}. Skipping received status update.", event.poId());
            }

        } catch (Exception e) {
            log.error("Failed to sync bill to Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onReturnApproved(ReturnApprovedEvent event) {
        log.info("Syncing Return to Zoho Credit Note: {}", event.requestId());
        try {
            // Derive customer name from email prefix
            String customerName = event.email().contains("@")
                ? event.email().substring(0, event.email().indexOf('@')) : event.email();
            String customerId = zohoClient.findOrCreateCustomer(customerName, event.email());

            // Distribute refund amount proportionally across returned items
            int totalReturnedQty = event.items().stream().mapToInt(ReturnApprovedEvent.ApprovedReturnItem::quantity).sum();
            double perUnitRefund = totalReturnedQty > 0 ? event.refundAmount() / totalReturnedQty : event.refundAmount();

            List<Map<String, Object>> lineItems = event.items().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", "Return: " + item.itemCode());
                    map.put("rate", perUnitRefund);
                    map.put("quantity", item.quantity());
                    return map;
                }).collect(Collectors.toList());

            Map<String, Object> creditNote = new HashMap<>();
            creditNote.put("customer_id", customerId);
            creditNote.put("line_items", lineItems);
            creditNote.put("date", java.time.LocalDate.now().toString());
            creditNote.put("reference_number", "RET-" + event.requestId());

            // Link credit note to original Zoho Invoice if mapping exists
            Optional<ZohoEntityMapping> invoiceMapping = zohoMappingRepo.findByEntityTypeAndLocalId(
                ZohoEntityType.INVOICE, String.valueOf(event.orderId()));
            if (invoiceMapping.isPresent()) {
                creditNote.put("invoice_id", invoiceMapping.get().getZohoId());
                log.info("Credit Note linked to Zoho Invoice: {}", invoiceMapping.get().getZohoId());
            }

            zohoClient.createCreditNote(creditNote);
        } catch (Exception e) {
            log.error("Failed to sync credit note to Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Transactional
    public void onQuoteRequested(QuoteRequestedEvent event) {
        log.info("Creating Estimate in Zoho for Quote Request: {}", event.requestId());
        try {
            String customerId = zohoClient.findOrCreateCustomer(
                event.customerName(),
                event.customerEmail()
            );

            List<Map<String, Object>> lineItems = event.items().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", item.getProductName());
                    map.put("rate", item.getPrice());
                    map.put("quantity", item.getQuantity());
                    return map;
                }).collect(Collectors.toList());

            Map<String, Object> estimate = new HashMap<>();
            estimate.put("customer_id", customerId);
            estimate.put("line_items", lineItems);
            estimate.put("date", event.requestedAt().toLocalDate().toString());
            estimate.put("expiry_date", event.requestedAt().plusDays(30).toLocalDate().toString());
            estimate.put("reference_number", "QUOTE-" + event.requestId());

            String estimateId = zohoClient.createEstimate(estimate);

            // Persist Zoho Estimate ID for quote acceptance flow
            zohoMappingRepo.save(ZohoEntityMapping.create(
                ZohoEntityType.ESTIMATE, String.valueOf(event.requestId()), estimateId));

            log.info("Estimate created and mapped in Zoho: {}", estimateId);

        } catch (Exception e) {
            log.error("Failed to create estimate in Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Transactional
    public void onQuoteAccepted(QuoteAcceptedEvent event) {
        log.info("Marking Estimate as accepted in Zoho: {}", event.quoteId());
        try {
            // Look up the stored Zoho Estimate ID
            Optional<ZohoEntityMapping> estimateMapping = zohoMappingRepo.findByEntityTypeAndLocalId(
                ZohoEntityType.ESTIMATE, String.valueOf(event.quoteId()));

            if (estimateMapping.isEmpty()) {
                log.warn("No Zoho Estimate mapping found for Quote {}. Cannot mark as accepted.", event.quoteId());
                return;
            }

            String zohoEstimateId = estimateMapping.get().getZohoId();

            // 1. Mark the estimate as accepted in Zoho
            zohoClient.markEstimateAccepted(zohoEstimateId);
            log.info("Zoho Estimate {} marked as accepted", zohoEstimateId);

            // 2. Convert the accepted estimate to a Sales Order
            String zohoSoId = zohoClient.convertEstimateToSalesOrder(zohoEstimateId);
            log.info("Zoho Estimate {} converted to Sales Order {}", zohoEstimateId, zohoSoId);

            // 3. Store the secondary ID (Sales Order) on the estimate mapping
            ZohoEntityMapping mapping = estimateMapping.get();
            mapping.setZohoSecondaryId(zohoSoId);
            mapping.setUpdatedAt(java.time.LocalDateTime.now());
            zohoMappingRepo.save(mapping);

            // 4. Also create a Sales Order mapping for the associated order
            if (event.orderId() != null) {
                zohoMappingRepo.save(ZohoEntityMapping.create(
                    ZohoEntityType.SALES_ORDER, String.valueOf(event.orderId()), zohoSoId));
            }

        } catch (Exception e) {
            log.error("Failed to process quote acceptance in Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onExpenseIncurred(ExpenseIncurredEvent event) {
        log.info("Recording expense in Zoho: {}", event.expenseId());
        try {
            Map<String, Object> expense = new HashMap<>();
            expense.put("account_name", event.category());
            expense.put("date", event.incurredAt().toLocalDate().toString());
            expense.put("amount", event.amount());
            expense.put("description", event.description());
            expense.put("reference_number", "EXP-" + event.expenseId());
            expense.put("paid_through_account_name", event.paymentMethod());

            zohoClient.createExpense(expense);
        } catch (Exception e) {
            log.error("Failed to record expense in Zoho", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onStockUpdated(StockUpdatedEvent event) {
        // Only sync manual adjustments, not sales/purchases which are handled via Orders/Bills
        if (!"MANUAL".equalsIgnoreCase(event.source()) && !"CORRECTION".equalsIgnoreCase(event.source())) {
            return;
        }

        log.info("Syncing inventory adjustment to Zoho: {}", event.itemCode());
        try {
            String itemId = zohoClient.findItem(event.itemCode());
            if (itemId == null) {
                log.warn("Item not found in Zoho for adjustment: {}", event.itemCode());
                return;
            }

            int adjustmentQty = event.newQuantity() - event.oldQuantity();
            
            Map<String, Object> lineItem = new HashMap<>();
            lineItem.put("item_id", itemId);
            lineItem.put("quantity_adjusted", adjustmentQty);
            // lineItem.put("adjustment_account_id", ...); // Optional, uses default if not set

            Map<String, Object> adjustment = new HashMap<>();
            adjustment.put("date", event.updatedAt().toLocalDate().toString());
            adjustment.put("reason", "Stock Correction: " + event.source());
            adjustment.put("adjustment_type", "quantity");
            adjustment.put("line_items", List.of(lineItem));

            zohoClient.adjustInventory(adjustment);

        } catch (Exception e) {
            log.error("Failed to sync inventory adjustment", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onVendorReturn(VendorReturnEvent event) {
        log.info("Syncing Vendor Return to Zoho Credit: {}", event.returnId());
        try {
            String vendorId = zohoClient.findOrCreateVendor(event.vendorName());

            List<Map<String, Object>> lineItems = event.items().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    // Ideally find item_id, but name might suffice for credit note if loosely coupled
                    String itemId = zohoClient.findItem(item.itemCode());
                    if (itemId != null) map.put("item_id", itemId);
                    
                    map.put("name", item.itemName());
                    map.put("rate", item.unitPrice());
                    map.put("quantity", item.quantity());
                    return map;
                }).collect(Collectors.toList());

            Map<String, Object> credit = new HashMap<>();
            credit.put("vendor_id", vendorId);
            credit.put("line_items", lineItems);
            credit.put("creditnote_number", "VCN-" + event.returnId());
            credit.put("date", event.returnedAt().toLocalDate().toString());
            credit.put("reference_number", event.poNumber());

            zohoClient.createVendorCredit(credit);

        } catch (Exception e) {
            log.error("Failed to sync vendor return", e);
        }
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void onJournalEntryPosted(JournalEntryPostedEvent event) {
        log.info("Syncing Manual Journal to Zoho: {}", event.journalId());
        try {
            List<Map<String, Object>> chartOfAccounts = zohoClient.getChartOfAccounts();
            
            // Helper to find Account ID by Name
            java.util.function.Function<String, String> findAccountId = name -> chartOfAccounts.stream()
                .filter(a -> name.equalsIgnoreCase((String)a.get("account_name")))
                .map(a -> (String) a.get("account_id"))
                .findFirst()
                .orElse(null);

            List<Map<String, Object>> lines = event.lines().stream()
                .map(line -> {
                    String accountId = findAccountId.apply(line.accountName());
                    if (accountId == null) {
                        log.warn("Account not found in Zoho: {}", line.accountName());
                        return null; 
                    }
                    
                    Map<String, Object> map = new HashMap<>();
                    map.put("account_id", accountId);
                    map.put("debit_or_credit", line.debit().compareTo(java.math.BigDecimal.ZERO) > 0 ? "debit" : "credit");
                    map.put("amount", line.debit().compareTo(java.math.BigDecimal.ZERO) > 0 ? line.debit() : line.credit());
                    map.put("description", line.description());
                    return map;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            if (lines.isEmpty()) {
                log.warn("No valid lines for journal entry");
                return;
            }

            Map<String, Object> journal = new HashMap<>();
            journal.put("journal_date", event.entryDate().toString());
            journal.put("reference_number", event.referenceNumber());
            journal.put("notes", event.description());
            journal.put("line_items", lines);

            zohoClient.createJournal(journal);

        } catch (Exception e) {
            log.error("Failed to sync journal entry", e);
        }
    }
}

