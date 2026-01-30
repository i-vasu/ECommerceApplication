# System Audit Report: Event Decoupling

**Date:** 2026-01-29
**Status:** In Progress
**Objective:** Identify direct dependencies between modules and propose event-driven solutions.

## Executive Summary
Audit reveals significant coupling in `modulith-order` where event listeners (`OrderCleanupListener`, `OrderFulfillmentListener`) explicitly depend on services from `logistics`, `finance`, and `erp-sync`. While they use events *internally* to trigger, the implementation is centralized in the wrong module, violating the "Inversion of Control" principle of modularity.

## Findings

### 1. Misplaced Listeners in `modulith-order`
The Following listeners reside in `modulith-order` but orchestrate logic for external modules.

| Listener | Trigger Event | Action | Target Module | Violation Payload |
| :--- | :--- | :--- | :--- | :--- |
| `OrderFulfillmentListener` | `OrderPaidEvent` | `shipmentService.createShipment` | **Logistics** | `ShipmentService` injected into Order module |
| `OrderCleanupListener` | `OrderCancelledEvent` | `shipmentService.cancelShipment` | **Logistics** | `ShipmentService` injected into Order module |
| `OrderCleanupListener` | `OrderCancelledEvent` | `paymentService.initiateRefund` | **Finance** | `PaymentService` injected into Order module |
| `OrderCleanupListener` | `OrderCancelledEvent` | `erpNextService.cancelSalesOrder` | **ERP-Sync** | `ERPNextService` injected into Order module |

### 2. Direct Service Dependencies in `OrderServiceImpl`
*   `PaymentService` is used directly. (Assessment: Likely required for synchronous Payment Link generation, acceptable for now if interface-based, but could be refactored to an Event-Request pattern if strict decoupling is desired).
*   `InventoryReservationService`. (Assessment: Likely acceptable for synchronous consistency check).

## Recommendations

### Phase 1: Distribute Listeners (High Priority)
Move the logic from `modulith-order` listeners to the respective "Owner" modules. `modulith-order` should simply publish the event and not care who listens.

1.  **Create `LogisticsEventListener` in `modulith-logistics`**
    *   Subscribe to `OrderPaidEvent` -> Create Shipment.
    *   Subscribe to `OrderCancelledEvent` -> Cancel Shipment.
2.  **Create `FinanceEventListener` in `modulith-finance`**
    *   Subscribe to `OrderCancelledEvent` -> Initiate Refund.
3.  **Create `ERPEventListener` in `modulith-erp-sync`**
    *   Subscribe to `OrderCancelledEvent` -> Cancel ERP Sales Order.

### Phase 2: Cleanup
1.  Remove `OrderFulfillmentListener` and `OrderCleanupListener` from `modulith-order`.
2.  Remove `ShipmentService`, `PaymentService`, `ERPNextService` imports/dependencies from `modulith-order`.

## Implementation Plan
1.  **Logistics:** Implement `LogisticsEventListener`.
2.  **Finance:** Implement `FinanceEventListener`.
3.  **ERP:** Implement `ERPEventListener`.
4.  **Order:** Delete `OrderFulfillmentListener`, `OrderCleanupListener`.
5.  **Verify:** Dependency graph check.
