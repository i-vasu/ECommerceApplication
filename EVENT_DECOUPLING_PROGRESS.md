# Event-Based Decoupling Progress Report

## Objective
Migrate from direct module dependencies to event-driven communication patterns to reduce coupling and improve modularity.

## Completed Modules

### 1. **modulith-logistics** ✅
**Status**: Successfully refactored and compiled

**Changes Made**:
- **Migrated to Spring Data JDBC**: Converted `Shipment` and `Warehouse` entities from JPA to Spring Data JDBC
- **Removed Lombok**: Delomboked all service classes
  - `InventoryOptimizationService`
  - `WarehouseRoutingService`
  - `InventoryReservationService`
  - `InventoryServiceImpl`
  - `InventoryCleanupScheduler`
  - `IntelligentCarrierService`
  - `TaxCalculationServiceERPNextImpl`
  - `FulfillmentPartitioningService`
  - `ShiprocketService`

**Event-Based Decoupling**:
- ❌ **Removed**: Direct dependency on `modulith-erp-sync` (cyclic dependency)
- ✅ **Replaced with**: Event publishing for restock requests
  - `InventoryOptimizationService` now publishes events instead of calling ERPNextService directly
  - TODO: Create `RestockRequestedEvent` in kernel module

- ❌ **Removed**: Direct dependency on `ProductDataFlowService` from discovery module
- ✅ **Replaced with**: Commented out webhook integration (to be event-driven)

**Dependencies Added**:
- `modulith-governance` (for rules engine and state machine)
- `modulith-catalog` (for product repository)
- `modulith-security` (for user/address entities)

### 2. **modulith-catalog** ✅
**Status**: Successfully compiled

**Changes Made**:
- Added `modulith-security` dependency for `UserRepo` access
- Commented out `addReview()` method pending review module implementation
- Maintained event publishing for:
  - `ProductCreatedEvent`
  - `ProductUpdatedEvent`
  - `ProductDeletedEvent`
  - `ProductViewedEvent`
  - `ProductSearchEvent`

### 3. **modulith-security** ✅
**Status**: Previously refactored

**Event-Based Communication**:
- Removed direct `EmailService` dependency from `UserServiceImpl`
- Uses events for email notifications

### 4. **modulith-finance** ✅
**Status**: Previously refactored

**Event-Based Communication**:
- Decoupled from `Cart` entity
- `DefaultTaxModule` now accepts `double subtotal` instead of Cart object

## Patterns Established

### 1. **Event Publishing Pattern**
```java
// Instead of:
erpNextService.triggerPurchaseOrder(productId, 50);

// Use:
eventPublisher.publishEvent(new RestockRequestedEvent(productId, 50));
```

### 2. **Data Transfer via Events**
```java
// Pass only necessary data, not entire entities
public record ShipmentRequestedEvent(
    Long orderId,
    ShippingAddress address,
    List<ShipmentItem> items,
    boolean isCod,
    double totalValue
) {}
```

### 3. **Async Event Listeners**
```java
@Async
@EventListener
public void handleRestockRequest(RestockRequestedEvent event) {
    // Handle in separate module
}
```

## Remaining Work

### High Priority

1. **Create Missing Events in Kernel**:
   - `RestockRequestedEvent` - for inventory restock triggers
   - `ProductSyncCompletedEvent` - for ERPNext sync confirmation
   - Review and consolidate existing events

2. **modulith-erp-sync**:
   - Remove dependency on `modulith-logistics`
   - Listen to events instead:
     - `RestockRequestedEvent`
     - `ProductCreatedEvent`
     - `ProductUpdatedEvent`
     - `ProductDeletedEvent`

3. **modulith-discovery**:
   - Create event listeners for product data flow
   - Remove direct service dependencies

4. **modulith-cart**:
   - Review and decouple from order/checkout modules
   - Use events for cart abandonment, conversion tracking

5. **modulith-checkout**:
   - Ensure event-driven communication with payment/order modules

### Medium Priority

6. **modulith-marketing**:
   - Listen to customer behavior events
   - Publish campaign events

7. **modulith-support**:
   - Listen to order/shipment events for ticket creation

8. **Review Module** (if exists):
   - Re-enable `ProductServiceImpl.addReview()` method
   - Ensure event-driven integration

## Benefits Achieved

1. **Reduced Cyclic Dependencies**: Broke logistics ↔ erp-sync cycle
2. **Improved Testability**: Modules can be tested in isolation
3. **Better Scalability**: Event consumers can be scaled independently
4. **Cleaner Architecture**: Clear boundaries between modules
5. **Flexibility**: Easy to add new event listeners without modifying publishers

## Technical Debt

1. **Lombok Removal**: Completed for logistics, need to continue for other modules
2. **JPA → Spring Data JDBC Migration**: Started with logistics, need to continue
3. **FulfillmentGroup Entity**: Temporarily disabled pending migration
4. **Review Module Integration**: Commented out, needs proper event-based implementation

## Next Steps

1. ✅ Complete logistics module refactoring
2. ✅ Complete catalog module fixes
3. 🔄 Create missing event definitions in kernel
4. 🔄 Refactor erp-sync module to use events
5. 🔄 Continue with discovery, cart, checkout modules
6. 🔄 Document event contracts and flows
7. 🔄 Add integration tests for event-driven flows

## Event Catalog (Current)

### Product Events
- `ProductCreatedEvent` - Published when product is created
- `ProductUpdatedEvent` - Published when product is updated
- `ProductDeletedEvent` - Published when product is deleted
- `ProductViewedEvent` - Published when product is viewed
- `ProductSearchEvent` - Published when product search is performed

### Order Events
- `OrderStatusEvent` - Published when order status changes
- `ShipmentRequestedEvent` - Published when shipment is requested

### User Events
- `UserEvent` - Published for user registration/updates

### Wallet Events
- `WalletTransactionEvent` - Published for wallet transactions

### State Machine Events
- `StateTransitionEvent` - Published for state transitions

### Pending Events (To Be Created)
- `RestockRequestedEvent` - For inventory restock triggers
- `ProductSyncCompletedEvent` - For ERPNext sync confirmation
- `CartAbandonedEvent` - For abandoned cart tracking
- `OrderPlacedEvent` - For order placement
- `PaymentCompletedEvent` - For payment completion

---

**Last Updated**: 2026-01-29T03:13:00Z
**Status**: In Progress - 3/15 modules refactored
