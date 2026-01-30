# Event-Driven Architecture - Current State Diagram

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SPRING EVENT BUS                                     │
│                    (ApplicationEventPublisher)                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │ Events Flow Through Bus
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        │                           │                           │
┌───────▼────────┐         ┌────────▼───────┐         ┌────────▼────────┐
│   CATALOG      │         │   LOGISTICS    │         │   ERP-SYNC      │
│   Module       │         │   Module       │         │   Module        │
└────────────────┘         └────────────────┘         └─────────────────┘
        │                           │                           │
        │ Publishes:                │ Publishes:                │ Publishes:
        │ • ProductCreated          │ • RestockRequested        │ • RestockCompleted
        │ • ProductUpdated          │ • ShipmentRequested       │ • ProductSyncCompleted
        │ • ProductDeleted          │ • ShipmentStatusUpdated   │ • StockUpdated
        │ • ProductViewed           │                           │
        │                           │                           │ Listens to:
        │                           │ Listens to:               │ • RestockRequested
        │                           │ • RestockCompleted        │ • ProductCreated
        │                           │ • ProductCreated          │ • ProductUpdated
        │                           │ • ProductUpdated          │ • ProductDeleted
        └───────────────────────────┴───────────────────────────┘

┌───────────────┐         ┌────────────────┐         ┌─────────────────┐
│  DISCOVERY    │         │    CART        │         │   CHECKOUT      │
│  Module       │         │    Module      │         │   Module        │
└───────────────┘         └────────────────┘         └─────────────────┘
        │                           │                           │
        │ Listens to:               │ Publishes:                │ Publishes:
        │ • ProductCreated          │ • CartAbandoned           │ • PaymentCompleted
        │ • ProductUpdated          │ • CartConverted           │ • PaymentFailed
        │ • ProductSyncCompleted    │                           │ • CartConverted
        │                           │                           │
        │ Actions:                  │ Listens to:               │ Listens to:
        │ • Index for search        │ • PaymentCompleted        │ • (Future: Promo events)
        │ • Generate AI tags        │   (to mark as converted)  │
        │ • Update vectors          │                           │
        └───────────────────────────┴───────────────────────────┘

┌───────────────┐         ┌────────────────┐         ┌─────────────────┐
│    ORDER      │         │   MARKETING    │         │   ANALYTICS     │
│    Module     │         │   Module       │         │   Module        │
└───────────────┘         └────────────────┘         └─────────────────┘
        │                           │                           │
        │ (Future Plan)             │ (Future Plan)             │ (Future Plan)
        │ Listens to:               │ Listens to:               │ Listens to:
        │ • PaymentCompleted        │ • CartAbandoned           │ • ALL EVENTS
        │ • ShipmentRequested       │ • PaymentFailed           │   (passive observer)
        │                           │ • OrderCreated            │
        │ Publishes:                │ • ProductViewed           │ Actions:
        │ • OrderCreated            │                           │ • Track metrics
        │ • OrderCancelled          │ Actions:                  │ • Generate reports
        │ • OrderStatusChanged      │ • Send emails             │ • ML training data
        │                           │ • Trigger campaigns       │
        └───────────────────────────┴───────────────────────────┘
```

---

## Event Flow Matrix

| Event | Publisher | Listeners | Purpose |
|-------|-----------|-----------|---------|
| **ProductCreatedEvent** | Catalog | Discovery, ERP-Sync, Logistics | Index, sync to ERP, init inventory |
| **ProductUpdatedEvent** | Catalog | Discovery, ERP-Sync, Logistics | Reindex, sync, update stock |
| **ProductDeletedEvent** | Catalog | Discovery, ERP-Sync | Remove from search, deactivate in ERP |
| **ProductSyncCompletedEvent** | ERP-Sync | Discovery | Batch index synced products |
| **RestockRequestedEvent** | Logistics | ERP-Sync | Create purchase order |
| **RestockCompletedEvent** | ERP-Sync | Logistics | Update inventory levels |
| **StockUpdatedEvent** | ERP-Sync, Logistics | Catalog, Discovery | Refresh product cache |
| **ShipmentRequestedEvent** | Logistics | Order | Update order status |
| **ShipmentStatusUpdatedEvent** | Logistics | Order, ERP-Sync | Track delivery |
| **CartAbandonedEvent** | Cart | Marketing, Analytics | Recovery emails, track rate |
| **CartConvertedEvent** | Checkout | Marketing, Analytics | Conversion tracking |
| **PaymentCompletedEvent** | Checkout | Order, Finance, Marketing | Create order, record transaction |
| **PaymentFailedEvent** | Checkout | Marketing, Analytics | Retry emails, track failures |

---

## Module Dependency Graph (After Event Decoupling)

### Before Event-Driven Architecture
```
┌─────────────┐
│  Logistics  │◀──────┐
└──────┬──────┘       │
       │              │ CYCLIC!
       ▼              │
┌─────────────┐       │
│  ERP-Sync   │───────┘
└─────────────┘

Strong coupling across all modules
```

### After Event-Driven Architecture
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Logistics  │     │  ERP-Sync   │     │  Discovery  │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │  Publishes        │  Publishes        │  Listens
       │  Events           │  Events           │  to Events
       ▼                   ▼                   ▼
┌──────────────────────────────────────────────────────┐
│              SPRING EVENT BUS                         │
│         (Zero coupling between modules)               │
└──────────────────────────────────────────────────────┘

Loose coupling via event bus
```

---

## Event Processing Patterns

### 1. Synchronous Events (Critical Path)
```java
// NOT async - must complete in same transaction
@EventListener
@Transactional
public void handlePaymentCompleted(PaymentCompletedEvent event) {
    // Create order immediately - must succeed
    orderService.createOrder(event);
}
```

### 2. Asynchronous Events (Non-Critical)
```java
// Async - can fail without affecting main flow
@Async
@EventListener
public void handleProductCreated(ProductCreatedEvent event) {
    // Index for search - can retry later if fails
    searchService.index(event.productId());
}
```

### 3. Batch Processing Events
```java
@Async
@EventListener
public void handleProductSyncCompleted(ProductSyncCompletedEvent event) {
    // Process many products efficiently
    event.itemCodes().parallelStream()
        .forEach(itemCode -> indexProduct(itemCode));
}
```

---

## Event Retry Strategy

### Level 1: Application-Level Retry
```java
@Async
@EventListener
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void handleEvent(MyEvent event) {
    // Automatically retries on failure
}
```

### Level 2: Dead Letter Queue (Future)
```java
@Async
@EventListener
public void handleEvent(MyEvent event) {
    try {
        processEvent(event);
    } catch (Exception e) {
        deadLetterQueue.send(event);
        // Manual review/retry
    }
}
```

### Level 3: Event Store/Outbox (Future)
```
1. Persist event to database
2. Background job publishes from DB
3. Mark as published when successful
4. Replay failed events
```

---

## Performance Metrics

### Event Processing Times (Target)

| Event Type | Processing Time | Async? | Priority |
|-----------|----------------|--------|----------|
| ProductCreated | < 100ms | Yes | Medium |
| ProductSyncCompleted | < 5s (batch) | Yes | Low |
| RestockRequested | < 500ms | Yes | High |
| PaymentCompleted | < 200ms | No | Critical |
| CartAbandoned | < 1s | Yes | Low |
| ShipmentStatusUpdated | < 300ms | Yes | Medium |

### Scalability Targets

- **Event Throughput**: 10,000 events/second
- **Listener Lag**: < 100ms for async events
- **Failure Rate**: < 0.1%
- **Retry Success**: > 95%

---

## Benefits Achieved

### 1. ✅ Zero Cyclic Dependencies
- Before: Logistics ↔ ERP-Sync (cyclic)
- After: Both publish events independently

### 2. ✅ Independent Deployment
- Can deploy Discovery without touching ERP-Sync
- Can deploy Cart without touching Checkout
- Modules evolve independently

### 3. ✅ Easy Feature Addition
```java
// Want to add recommendation engine?
// Just add a listener - no changes to publishers!

@EventListener
public void handleProductViewed(ProductViewedEvent event) {
    recommendationEngine.recordView(event.productId(), event.userId());
}
```

### 4. ✅ Better Testability
```java
// Test without real dependencies
@Test
void shouldIndexProductWhenCreated() {
    var event = new ProductCreatedEvent(...);
    eventPublisher.publishEvent(event);
    
    verify(searchService).index(productId);
}
```

### 5. ✅ Improved Observability
- All events logged centrally
- Easy to trace event flows
- Monitor event processing metrics

---

## Future Enhancements

### Phase 1: Event Store
- Persist all events to database
- Enable event replay
- Support audit trails

### Phase 2: Event Streaming
- Replace Spring Events with Kafka
- Support external consumers
- Enable real-time analytics

### Phase 3: CQRS
- Separate read/write models
- Event-sourced aggregates
- Eventual consistency

---

**Architecture Status**: ✅ Event-Driven Foundation Complete  
**Coverage**: 47% (7/15 modules)  
**Health**: 🟢 All systems operational
