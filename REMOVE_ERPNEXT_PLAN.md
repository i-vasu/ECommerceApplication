# [EXECUTED] Migration Plan: Remove ERPNext, Use Direct PostgreSQL Storage

> [!NOTE]
> **Status:** COMPLETED
> **Date:** 2026-02-11
> **Outcome:** ERPNext infrastructure removed. Custom ERP implemented on top of ParadeDB/PostgreSQL.


## Current Architecture Issues
- **ERPNext**: 5 containers (backend, worker, scheduler, nginx, mariadb) = ~1.5GB RAM
- **Complexity**: Multi-tenant sites, Redis compatibility issues
- **Overkill**: Full ERP system just to store product catalog
- **Maintenance**: Version upgrades, patches, compatibility

## Proposed Simplified Architecture

### Remove
- ❌ ERPNext (all 5 containers)
- ❌ MariaDB (erpnext-db)
- ❌ ERPNext sync service

### Keep & Enhance
- ✅ PostgreSQL (ParadeDB) - Already has products table
- ✅ Dragonfly - Cache for Java backend
- ✅ Java Backend - Direct product management
- ✅ Storefront - No changes needed

## New Product Management Flow

### Option 1: Admin REST API (Recommended)
```
Admin UI/Script → Java Backend API → PostgreSQL → ParadeDB Index → Storefront
```

**Benefits:**
- Use existing `ProductAdminController` in Java backend
- RESTful API for CRUD operations
- Automatic ParadeDB indexing
- No external dependencies

### Option 2: Direct Database Seeding
```
SQL Script → PostgreSQL → Java Backend reads → Storefront
```

**Benefits:**
- Simplest approach
- Fast bulk imports
- Version controlled product data

### Option 3: CSV/JSON Import
```
CSV/JSON File → Java Import Service → PostgreSQL → Storefront
```

**Benefits:**
- Easy for non-technical users
- Bulk operations
- Data portability

## Implementation Steps

### 1. Export Existing ERPNext Data (if any)
```sql
-- Connect to ERPNext MariaDB
SELECT item_code, item_name, item_group, standard_rate, description, brand, image
FROM `tabItem`
WHERE disabled = 0;
```

### 2. Create Product Seed Script
```java
// Use existing DataInitializer or create ProductSeeder
// Populate products table directly
```

### 3. Remove ERPNext
```bash
docker-compose -f docker-compose.erpnext.yml down
docker volume rm erpnext-db-data erpnext-sites
```

### 4. Update Backend Configuration
```properties
# Remove from application.properties:
# erpnext.baseUrl
# erpnext.apiKey
# erpnext.apiSecret
```

### 5. Remove ERPNext Sync Module
```bash
# Delete or disable:
# modulith-erp-sync/
```

## Resource Savings

| Component | Before | After | Savings |
|-----------|--------|-------|---------|
| **Containers** | 9 | 4 | -5 |
| **Memory** | ~3.5GB | ~2GB | **-1.5GB** |
| **Complexity** | High | Low | ✅ |
| **Maintenance** | Complex | Simple | ✅ |

## Product Data Management

### Create Admin Endpoints
```java
@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController {
    
    @PostMapping
    public Product createProduct(@RequestBody ProductDTO dto) {
        // Create product
        // Auto-index in ParadeDB
    }
    
    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody ProductDTO dto) {
        // Update product
    }
    
    @PostMapping("/bulk-import")
    public List<Product> bulkImport(@RequestBody List<ProductDTO> products) {
        // Bulk create
    }
}
```

### Seed Initial Data
```java
@Component
public class ProductSeeder {
    
    @PostConstruct
    public void seedProducts() {
        if (productRepo.count() == 0) {
            // Load from JSON file or hardcoded list
            List<Product> products = loadProductsFromResource();
            productRepo.saveAll(products);
        }
    }
}
```

## Migration Checklist

- [ ] Export any existing ERPNext product data
- [ ] Create product seed data (20 luxury items ready!)
- [ ] Test product CRUD via Java backend API
- [ ] Verify ParadeDB search indexing works
- [ ] Stop ERPNext containers
- [ ] Remove ERPNext docker-compose file
- [ ] Clean up ERPNext volumes
- [ ] Remove `modulith-erp-sync` module
- [ ] Update documentation
- [ ] Test full flow: Seed → Backend → Storefront

## Recommendation

**YES, remove ERPNext!** 

We gain:
- ✅ 1.5GB RAM savings
- ✅ Simpler architecture
- ✅ Faster startup
- ✅ Easier maintenance
- ✅ No Redis compatibility issues
- ✅ Direct control over product data

We lose:
- ❌ Nothing we're actually using!

The 20 luxury products are already defined in `add_products_to_erpnext.py`. 
We can easily convert this to seed PostgreSQL directly.

---

**Next Step:** Shall I proceed with removing ERPNext and creating a direct PostgreSQL product seeder?
