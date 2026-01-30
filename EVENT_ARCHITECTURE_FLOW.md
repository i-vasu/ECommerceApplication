# Event-Driven Architecture Flow

## Current Event Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         EVENT BUS (Spring Events)                    │
└─────────────────────────────────────────────────────────────────────┘
                                    ▲
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        │                           │                           │
┌───────▼────────┐         ┌────────▼───────┐         ┌────────▼────────┐
│   CATALOG      │         │   LOGISTICS    │         │   ERP-SYNC      │
│   MODULE       │         │   MODULE       │         │   MODULE        │
└────────────────┘         └────────────────┘         └─────────────────┘
        │                           │                           │
        │ Publishes:                │ Publishes:                │ Listens to:
        │ • ProductCreatedEvent     │ • ShipmentRequestedEvent  │ • RestockRequestedEvent
        │ • ProductUpdatedEvent     │ • InventoryLowEvent       │ • ProductSyncRequestedEvent
        │ • ProductDeletedEvent     │                           │ • OrderCreatedEvent
        │ • ProductViewedEvent      │ Listens to:               │
        │ • ProductSearchEvent      │ • OrderPlacedEvent        │ Publishes:
        │                           │ • RestockCompletedEvent   │ • ProductSyncedEvent
        │                           │                           │ • StockUpdatedEvent
        └───────────────────────────┴───────────────────────────┘
```

## Event Flow Examples

### Example 1: Product Creation Flow

```
1. Admin creates product in Catalog
   ↓
2. Catalog publishes ProductCreatedEvent
   ↓
3. Discovery Module listens → indexes product for search
   ↓
4. Marketing Module listens → creates promotional campaign
   ↓
5. Analytics Module listens → tracks product metrics
```

### Example 2: Inventory Restock Flow

```
1. Logistics detects low inventory
   ↓
2. Logistics publishes RestockRequestedEvent
   ↓
3. ERP-Sync Module listens → triggers purchase order in ERPNext
   ↓
4. ERPNext confirms PO
   ↓
5. ERP-Sync publishes RestockCompletedEvent
   ↓
6. Logistics listens → updates inventory status
```

### Example 3: Order Placement Flow

```
1. User places order in Checkout
   ↓
2. Checkout publishes OrderPlacedEvent
   ↓
3. Logistics listens → creates shipment request
   ↓
4. Finance listens → processes payment
   ↓
5. Marketing listens → triggers confirmation email
   ↓
6. Analytics listens → tracks conversion
```

## Event Definitions

### Product Events (modulith-kernel)

```java
// Existing
public record ProductCreatedEvent(
    Long productId,
    String itemCode,
    String productName,
    BigDecimal price,
    Integer quantity
) {}

public record ProductUpdatedEvent(
    Long productId,
    String itemCode,
    BigDecimal oldPrice,
    BigDecimal newPrice,
    Integer oldQuantity,
    Integer newQuantity
) {}

public record ProductDeletedEvent(
    Long productId,
    String itemCode
) {}

// Needed
public record ProductSyncedEvent(
    String tenantId,
    List<String> itemCodes,
    LocalDateTime syncTime
) {}
```

### Inventory Events (modulith-kernel)

```java
// Needed
public record RestockRequestedEvent(
    Long productId,
    String itemCode,
    int requestedQuantity,
    String reason
) {}

public record RestockCompletedEvent(
    Long productId,
    String itemCode,
    int receivedQuantity,
    LocalDateTime completedAt
) {}

public record InventoryLowEvent(
    Long productId,
    String itemCode,
    int currentQuantity,
    int threshold
) {}

public record StockUpdatedEvent(
    String itemCode,
    int oldQuantity,
    int newQuantity,
    String source // "ERP", "MANUAL", "RESERVATION"
) {}
```

### Order Events (modulith-kernel)

```java
// Existing
public record OrderStatusEvent(
    Long orderId,
    OrderStatus oldStatus,
    OrderStatus newStatus,
    LocalDateTime timestamp
) {}

// Needed
public record OrderPlacedEvent(
    Long orderId,
    Long userId,
    BigDecimal totalAmount,
    List<OrderItemDTO> items,
    ShippingAddress address
) {}

public record OrderCancelledEvent(
    Long orderId,
    String reason,
    LocalDateTime cancelledAt
) {}
```

### Shipment Events (modulith-kernel)

```java
// Existing
public record ShipmentRequestedEvent(
    Long orderId,
    ShippingAddress address,
    List<ShipmentItem> items,
    boolean isCod,
    double totalValue,
    String email
) {}

// Needed
public record ShipmentStatusUpdatedEvent(
    Long shipmentId,
    Long orderId,
    String oldStatus,
    String newStatus,
    String trackingNumber,
    LocalDateTime updatedAt
) {}

public record ShipmentDeliveredEvent(
    Long shipmentId,
    Long orderId,
    LocalDateTime deliveredAt
) {}
```

## Event Listener Examples

### ERP-Sync Module Listeners

```java
@Component
public class ERPNextEventListener {
    
    @Async
    @EventListener
    public void handleRestockRequest(RestockRequestedEvent event) {
        log.info("Restock requested for product: {}", event.productId());
        // Trigger purchase order in ERPNext
        erpNextService.createPurchaseOrder(event.itemCode(), event.requestedQuantity());
    }
    
    @Async
    @EventListener
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Syncing new product to ERPNext: {}", event.productId());
        // Create item in ERPNext
        erpNextService.createItem(event);
    }
}
```

### Discovery Module Listeners

```java
@Component
public class SearchIndexListener {
    
    @Async
    @EventListener
    public void handleProductSynced(ProductSyncedEvent event) {
        log.info("Indexing products from ERP sync: {}", event.itemCodes().size());
        // Index products for search
        for (String itemCode : event.itemCodes()) {
            searchService.indexProduct(itemCode);
        }
    }
    
    @Async
    @EventListener
    public void handleProductUpdated(ProductUpdatedEvent event) {
        log.info("Re-indexing updated product: {}", event.productId());
        searchService.reindexProduct(event.productId());
    }
}
```

### Logistics Module Listeners

```java
@Component
public class ShipmentEventListener {
    
    @Async
    @EventListener
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Creating shipment for order: {}", event.orderId());
        // Create shipment request
        shipmentService.createShipment(event);
    }
    
    @Async
    @EventListener
    public void handleRestockCompleted(RestockCompletedEvent event) {
        log.info("Updating inventory for restocked item: {}", event.itemCode());
        // Update inventory levels
        inventoryService.updateStock(event.itemCode(), event.receivedQuantity());
    }
}
```

## Benefits of Event-Driven Architecture

### 1. **Loose Coupling**
- Modules don't need to know about each other
- Easy to add/remove event listeners
- No direct dependencies between business modules

### 2. **Scalability**
- Event consumers can be scaled independently
- Async processing improves throughput
- Easy to add message queues (Kafka, RabbitMQ) later

### 3. **Flexibility**
- New features can listen to existing events
- Easy to add audit trails, analytics
- Support for event sourcing patterns

### 4. **Testability**
- Mock event publisher in tests
- Test event listeners in isolation
- Easy to verify event publishing

### 5. **Observability**
- Centralized event logging
- Track event flow across modules
- Monitor event processing metrics

## Migration Strategy

### Phase 1: Define Events ✅
- Create event records in kernel module
- Document event contracts
- Establish naming conventions

### Phase 2: Publish Events ✅
- Update modules to publish events
- Maintain backward compatibility
- Add event logging

### Phase 3: Implement Listeners 🔄
- Create event listeners in consuming modules
- Test event flow end-to-end
- Remove direct service calls

### Phase 4: Optimize ⏳
- Add event persistence (outbox pattern)
- Implement retry logic
- Add monitoring and alerting

---

**Last Updated**: 2026-01-29T03:16:00Z
