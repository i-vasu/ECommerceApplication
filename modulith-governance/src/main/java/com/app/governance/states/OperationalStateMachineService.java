package com.app.governance.states;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;

/**
 * Unified Operational State Machine Service.
 * Provides a single point of entry for triggering state transitions across all
 * modules.
 */
@Service
public class OperationalStateMachineService {

    private static final Logger log = LogManager.getLogger(OperationalStateMachineService.class);

    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final StateMachineFactory<OrderState, OrderEvent> orderStateMachineFactory;
    private final StateMachineFactory<PaymentState, PaymentEvent> paymentStateMachineFactory;
    private final StateMachineFactory<ReturnState, ReturnEvent> returnStateMachineFactory;
    private final StateMachineFactory<InventoryState, InventoryEvent> inventoryStateMachineFactory;
    private final StateMachineFactory<ShipmentState, ShipmentEvent> shipmentStateMachineFactory;
    private final StateMachineFactory<ProductState, ProductEvent> productStateMachineFactory;
    private final StateMachineFactory<AccountState, AccountEvent> accountStateMachineFactory;
    private final StateMachineFactory<PimState, PimEvent> pimStateMachineFactory;
    private final StateMachineFactory<ReviewState, ReviewEvent> reviewStateMachineFactory;
    private final StateMachineFactory<TicketState, TicketEvent> ticketStateMachineFactory;
    private final StateMachineFactory<WalletState, WalletEvent> walletStateMachineFactory;
    private final StateMachineFactory<FlashSaleState, FlashSaleEvent> flashSaleStateMachineFactory;
    private final StateMachineFactory<VendorState, VendorEvent> vendorStateMachineFactory;
    private final StateMachineFactory<CartState, CartEvent> cartStateMachineFactory;
    private final com.app.governance.audit.OperationalAuditRepo auditRepo;
    private final org.springframework.statemachine.persist.StateMachinePersister<Object, Object, String> persister;
    private final com.app.core.events.OutboxRepo outboxRepo;
    private final tools.jackson.databind.ObjectMapper objectMapper;
    private final org.springframework.statemachine.data.jpa.JpaStateMachineRepository jpaStateMachineRepository;

    public OperationalStateMachineService(
            org.springframework.context.ApplicationEventPublisher eventPublisher,
            StateMachineFactory<OrderState, OrderEvent> orderStateMachineFactory,
            StateMachineFactory<PaymentState, PaymentEvent> paymentStateMachineFactory,
            StateMachineFactory<ReturnState, ReturnEvent> returnStateMachineFactory,
            StateMachineFactory<InventoryState, InventoryEvent> inventoryStateMachineFactory,
            StateMachineFactory<ShipmentState, ShipmentEvent> shipmentStateMachineFactory,
            StateMachineFactory<ProductState, ProductEvent> productStateMachineFactory,
            StateMachineFactory<AccountState, AccountEvent> accountStateMachineFactory,
            StateMachineFactory<PimState, PimEvent> pimStateMachineFactory,
            StateMachineFactory<ReviewState, ReviewEvent> reviewStateMachineFactory,
            StateMachineFactory<TicketState, TicketEvent> ticketStateMachineFactory,
            StateMachineFactory<WalletState, WalletEvent> walletStateMachineFactory,
            StateMachineFactory<FlashSaleState, FlashSaleEvent> flashSaleStateMachineFactory,
            StateMachineFactory<VendorState, VendorEvent> vendorStateMachineFactory,
            StateMachineFactory<CartState, CartEvent> cartStateMachineFactory,
            com.app.governance.audit.OperationalAuditRepo auditRepo,
            org.springframework.statemachine.persist.StateMachinePersister<Object, Object, String> persister,
            com.app.core.events.OutboxRepo outboxRepo,
            tools.jackson.databind.ObjectMapper objectMapper,
            org.springframework.statemachine.data.jpa.JpaStateMachineRepository jpaStateMachineRepository) {
        this.eventPublisher = eventPublisher;
        this.orderStateMachineFactory = orderStateMachineFactory;
        this.paymentStateMachineFactory = paymentStateMachineFactory;
        this.returnStateMachineFactory = returnStateMachineFactory;
        this.inventoryStateMachineFactory = inventoryStateMachineFactory;
        this.shipmentStateMachineFactory = shipmentStateMachineFactory;
        this.productStateMachineFactory = productStateMachineFactory;
        this.accountStateMachineFactory = accountStateMachineFactory;
        this.pimStateMachineFactory = pimStateMachineFactory;
        this.reviewStateMachineFactory = reviewStateMachineFactory;
        this.ticketStateMachineFactory = ticketStateMachineFactory;
        this.walletStateMachineFactory = walletStateMachineFactory;
        this.flashSaleStateMachineFactory = flashSaleStateMachineFactory;
        this.vendorStateMachineFactory = vendorStateMachineFactory;
        this.cartStateMachineFactory = cartStateMachineFactory;
        this.auditRepo = auditRepo;
        this.persister = persister;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
        this.jpaStateMachineRepository = jpaStateMachineRepository;
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerCartEvent(Long cartId, CartEvent event) {
        processEvent(cartStateMachineFactory.getStateMachine("CRT-" + cartId), event, "Cart", cartId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerOrderEvent(Long orderId, OrderEvent event) {
        processEvent(orderStateMachineFactory.getStateMachine("ORD-" + orderId), event, "Order", orderId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerPaymentEvent(Long paymentId, PaymentEvent event) {
        processEvent(paymentStateMachineFactory.getStateMachine("PAY-" + paymentId), event, "Payment", paymentId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerShipmentEvent(Long shipmentId, ShipmentEvent event) {
        processEvent(shipmentStateMachineFactory.getStateMachine("SHP-" + shipmentId), event, "Shipment", shipmentId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerReturnEvent(Long returnId, ReturnEvent event) {
        processEvent(returnStateMachineFactory.getStateMachine("RTN-" + returnId), event, "Return", returnId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerProductEvent(Long productId, ProductEvent event) {
        processEvent(productStateMachineFactory.getStateMachine("PRD-" + productId), event, "Product", productId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerAccountEvent(Long userId, AccountEvent event) {
        processEvent(accountStateMachineFactory.getStateMachine("ACC-" + userId), event, "Account", userId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerPimEvent(Long productId, PimEvent event) {
        processEvent(pimStateMachineFactory.getStateMachine("PIM-" + productId), event, "PIM", productId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerReviewEvent(Long reviewId, ReviewEvent event) {
        processEvent(reviewStateMachineFactory.getStateMachine("REV-" + reviewId), event, "Review", reviewId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerTicketEvent(Long ticketId, TicketEvent event) {
        processEvent(ticketStateMachineFactory.getStateMachine("TCK-" + ticketId), event, "Ticket", ticketId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerWalletEvent(Long walletId, WalletEvent event) {
        processEvent(walletStateMachineFactory.getStateMachine("WLT-" + walletId), event, "Wallet", walletId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerFlashSaleEvent(Long saleId, FlashSaleEvent event) {
        processEvent(flashSaleStateMachineFactory.getStateMachine("FLS-" + saleId), event, "FlashSale", saleId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerVendorEvent(Long vendorId, VendorEvent event) {
        processEvent(vendorStateMachineFactory.getStateMachine("VND-" + vendorId), event, "Vendor", vendorId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void triggerInventoryEvent(Long inventoryId, InventoryEvent event) {
        processEvent(inventoryStateMachineFactory.getStateMachine("INV-" + inventoryId), event, "Inventory",
                inventoryId);
    }

    @SuppressWarnings("unchecked")
    private <S, E> void processEvent(StateMachine<S, E> machine, E event, String entityType, Long id) {
        String machineId = machine.getId();
        if (machineId == null) {
            String prefix = entityType.length() >= 3 ? entityType.substring(0, 3).toUpperCase() : entityType.toUpperCase();
            machineId = prefix + "-" + id;
            log.warn("Machine ID was null from factory for {} ID {}, using generated ID: {}", entityType, id, machineId);
        }

        log.info("Triggering {} Event: {} for ID: {} (Machine ID: {})", entityType, event, id, machineId);
        try {
            if (jpaStateMachineRepository.existsById(machineId)) {
                persister.restore((StateMachine<Object, Object>) machine, machineId);
            } else {
                log.info("No persistent state found for {} ID {}, starting fresh.", entityType, id);
            }
        } catch (Exception e) {
            log.warn("Error restoring state for {} ID {}: {}", entityType, id, e.getMessage());
        }

        machine.start();
        S source = machine.getState().getId();
        boolean success = machine.sendEvent(event);
        S target = machine.getState().getId();

        // Dashboard Log
        auditRepo.save(com.app.governance.audit.OperationalAudit.builder()
                .type("STATE_TRANSITION")
                .category(entityType)
                .entityId(String.valueOf(id))
                .detail(source + " --[" + event + "]--> " + target)
                .success(success)
                .result(success ? "SUCCESS" : "FAILED")
                .build());

        if (success) {
            com.app.core.events.StateTransitionEvent transitionEvent = new com.app.core.events.StateTransitionEvent(
                    entityType, id, source, target, event, true);

            // 1. In-memory Event (For immediate local listeners)
            eventPublisher.publishEvent(transitionEvent);

            // 2. Transactional Outbox (For persistent external sync)
            try {
                outboxRepo.save(com.app.core.events.OutboxEvent.builder()
                        .aggregateType(entityType)
                        .aggregateId(String.valueOf(id))
                        .eventType(event.toString())
                        .payload(objectMapper.writeValueAsString(transitionEvent))
                        .build());
            } catch (Exception e) {
                log.error("Failed to save Outbox Event for {}: {}", entityType, e.getMessage());
            }

            try {
                persister.persist((StateMachine<Object, Object>) machine, machineId);
            } catch (Exception e) {
                log.error("Failed to persist state for {} (ID: {}): {}", entityType, id, e.getMessage());
                // If persistence fails, we should still consider it a success if the transition worked,
                // but for debugging we'll keep the log.
            }
        }

        if (!success) {
            log.error("Failed to transition {} (ID: {}) with event {}", entityType, id, event);
            throw new RuntimeException("Illegal state transition for " + entityType);
        }
        machine.stop();
    }
}
