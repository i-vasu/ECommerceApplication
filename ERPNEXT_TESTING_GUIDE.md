# ERPNext E2E Testing Guide

## Current Status
✅ ERPNext containers running (MariaDB, Backend, Worker, Scheduler)
✅ DragonFly DB connected
✅ Site creation in progress: `fashion-store.local`
⏳ Installing Frappe + ERPNext (takes 3-5 minutes)

## Next Steps After Installation

### 1. Enable API Access
```bash
# Set the site as default
docker exec erpnext-backend bench use fashion-store.local

# Enable API access
docker exec erpnext-backend bench --site fashion-store.local set-config developer_mode 1

# Create API keys
docker exec erpnext-backend bench --site fashion-store.local add-api-key Administrator --api-secret your-secret-key
```

### 2. Required DocTypes for E-Commerce Integration

The following DocTypes are already built into ERPNext:
- **Item** (Maps to Product)
- **Customer** (Maps to User)
- **Sales Order** (Maps to Order)
- **Payment Entry** (For payment tracking)
- **Delivery Note** (For shipping)

### 3. Test Data to Create

#### Items (Products)
1. Kanjivaram Blue Silk Saree
2. Mysore Red Silk Saree
3. Banarasi Gold Saree
4. Cotton Kurti - Floral
5. Designer Lehenga
6. Chiffon Saree - Green
7. Embroidered Anarkali
8. Handloom Cotton Saree
9. Georgette Party Wear Saree
10. Traditional Pattu Pavadai

#### Customers
1. Administrator (built-in)
2. Test Customer 1
3. Test Customer 2
4. Bulk Buyer
5. VIP Customer

#### Sales Orders
1. Order #1: 2 Sarees (Customer 1)
2. Order #2: 1 Lehenga (Customer 2)
3. Order #3: Bulk order - 5 items (Bulk Buyer)
4. Order #4: VIP order (VIP Customer)
5. Order #5: Mixed items (Customer 1)

### 4. E2E Test Flows

#### Flow 1: Product Sync (Java → ERPNext)
```
POST /api/products (Java Backend)
  ↓
Product saved to PostgreSQL
  ↓
Redis Stream Event Published
  ↓
marketplace-service consumes event
  ↓
Creates/Updates Item in ERPNext
  ↓
VERIFY: Item exists in ERPNext with correct data
```

#### Flow 2: Order Creation & Sync
```
POST /api/orders (Java Backend)
  ↓
Order saved with PENDING status
  ↓
Redis Event: ORDER_CREATED
  ↓
marketplace-service creates Sales Order in ERPNext
  ↓
VERIFY: Sales Order in ERPNext with correct line items
```

#### Flow 3: Inventory Sync (ERPNext → Java)
```
Update Item stock quantity in ERPNext
  ↓
ERPNext Webhook triggers
  ↓
Java middleware receives webhook
  ↓
Updates Product quantity in Java DB
  ↓
VERIFY: Product stock updated in Java
```

#### Flow 4: Payment Flow
```
User pays via Razorpay
  ↓
Payment Entry created in ERPNext
  ↓
Sales Order status → PAID
  ↓
VERIFY: Order status updated in both systems
```

### 5. API Endpoints for Testing

ERPNext API Base: `http://localhost:8000/api/resource/`

Key endpoints:
- GET/POST `/Item` - Products
- GET/POST `/Customer` - Customers
- GET/POST `/Sales Order` - Orders
- GET/POST `/Payment Entry` - Payments
- GET `/Doc` - Generic DocType access

### 6. Test Execution Plan

1. **Unit Tests**: Individual API endpoints
2. **Integration Tests**: Java ↔ ERPNext sync
3. **E2E Tests**: Complete order flow
4. **Performance Tests**: 100+ concurrent requests
5. **Failure Tests**: Network failures, rollbacks

## Credentials

- **Site URL**: http://localhost:8000 (after exposing port)
- **Admin Username**: Administrator
- **Admin Password**: admin
- **API Key**: (to be generated)
- **API Secret**: (to be generated)

## Troubleshooting

If ERPNext site creation fails:
```bash
# Check logs
docker logs erpnext-backend

# Restart services
docker-compose -f docker-compose.erpnext.yml restart

# Drop and recreate site
docker exec erpnext-backend bench drop-site fashion-store.local --mariadb-root-password admin --force
docker exec erpnext-backend bench new-site fashion-store.local --mariadb-root-password admin --admin-password admin --install-app erpnext
```
