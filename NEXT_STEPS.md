# ✅ Custom ERP Setup - Simplified Architecture

## 🚀 Status
- **ERPNext Removed:** Saved 1.5GB RAM & fixed Redis issues.
- **Product Data:** Pre-seeded with 20 luxury items in PostgreSQL.
- **Custom ERP:** Built directly into Java Backend (faster, lighter).

## 🛠️ Components
1. **PostgreSQL (ParadeDB):** Stores products & orders (Port 5432)
2. **Dragonfly (Redis):** Caching & Queues (Port 6379)
3. **Java Backend:** Custom ERP logic + API (Port 8080)
4. **Storefront:** Next.js UI (Port 3000)

## ⚡ Quick Start

### 1. Ensure Database is Running
```powershell
wsl docker ps
# If not running:
wsl docker compose up -d postgres dragonfly
```

### 2. Start Java Backend
```powershell
cd "C:\Users\alway\OneDrive\Desktop\Vasu\ECommerceApplication"
.\run-backend.sh
```
*Note: The backend will automatically seed the 20 luxury products if the catalog is empty.*

### 3. Start Storefront
```powershell
cd "C:\Users\alway\OneDrive\Desktop\Vasu\vaabhi-storefront"
npm run dev
```
Visit: [http://localhost:3000](http://localhost:3000)

## 🔍 Verification

### Check Product Data (Backend)
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/products" -UseBasicParsing
```

### Check Search Index (ParadeDB)
```powershell
# Search for "Sherwani"
Invoke-WebRequest -Uri "http://localhost:8080/api/products/search?q=Sherwani" -UseBasicParsing
```

## 📝 Admin Tasks (Future)

### Add New Product (Custom ERP API)
Use Postman or Curl to POST to `/api/admin/products`:
```json
{
  "name": "New Luxury Saree",
  "price": 15000,
  "description": "Handwoven silk saree...",
  "category": "Ethnic Wear"
}
```

### Inventory Management
Inventory tracking is now managed directly in the backend. 
Future updates will add specific endpoints for stock adjustments.
