package com.app.logistics.inventory;

import com.app.logistics.inventory.entities.Inventory;
import com.app.logistics.inventory.entities.InventoryId;
import com.app.logistics.inventory.entities.InventoryTransaction;
import com.app.logistics.inventory.repositories.InventoryRepository;
import com.app.logistics.inventory.repositories.InventoryTransactionRepository;
import com.app.logistics.inventory.repositories.WarehouseRepository;
import com.app.logistics.inventory.repositories.BinRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "inventory")
@Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public class InventoryReservationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryReservationService.class);

    private final StringRedisTemplate redisTemplate;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final BinRepository binRepository;

    public InventoryReservationService(StringRedisTemplate redisTemplate,
                                       InventoryRepository inventoryRepository,
                                       InventoryTransactionRepository transactionRepository,
                                       WarehouseRepository warehouseRepository,
                                       BinRepository binRepository) {
        this.redisTemplate = redisTemplate;
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.warehouseRepository = warehouseRepository;
        this.binRepository = binRepository;
    }

    private static final String INVENTORY_KEY_PREFIX = "inventory:stock:";
    private static final String RESERVATION_KEY_PREFIX = "inventory:reservation:";

    // Lua script for atomic stock check-and-decrement
    private static final String RESERVE_SCRIPT = "local stock = tonumber(redis.call('get', KEYS[1])); " +
            "if stock == nil then return -1; end; " +
            "if stock >= tonumber(ARGV[1]) then " +
            "   return redis.call('decrby', KEYS[1], ARGV[1]); " +
            "else " +
            "   return -2; " +
            "end;";

    // Lua script for ATOMIC BATCH reservation (all or nothing)
    private static final String BATCH_RESERVE_SCRIPT = 
            "for i=1, #KEYS do " +
            "  local stock = tonumber(redis.call('get', KEYS[i])); " +
            "  if stock == nil or stock < tonumber(ARGV[i]) then " +
            "    return -1; " + // Signal failure for the whole batch
            "  end; " +
            "end; " +
            "for i=1, #KEYS do " +
            "  redis.call('decrby', KEYS[i], ARGV[i]); " +
            "end; " +
            "return 1;";

    // Lua script for Flash Sale atomic decrement
    private static final String FLASH_RESERVE_SCRIPT = 
            "local key = 'flash:' .. ARGV[1] .. ':stock:' .. ARGV[2]; " +
            "local stock = tonumber(redis.call('get', key)); " +
            "if stock == nil or stock < tonumber(ARGV[3]) then return 0; end; " +
            "return redis.call('decrby', key, ARGV[3]);";

    // 1. Initialize Stock in Redis (Read-Through from DB)
    public void setStock(Long warehouseId, Long binId, String itemCode, int quantity) {
        String key = INVENTORY_KEY_PREFIX + warehouseId + ":" + binId + ":" + itemCode;
        redisTemplate.opsForValue().set(key, String.valueOf(quantity), Duration.ofHours(24));
    }

    public void setFlashStock(Long productId, int quantity) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        String key = "flash:" + tenantId + ":stock:" + productId;
        redisTemplate.opsForValue().set(key, String.valueOf(quantity), Duration.ofHours(48));
    }

    public void initializeInventory(Long warehouseId, Long binId, String itemCode, int quantity) {
        InventoryId id = new InventoryId(itemCode, warehouseId, binId);
        if (inventoryRepository.findById(id).isEmpty()) {
            Inventory inv = new Inventory(itemCode, warehouseId, binId, quantity, 0, 0L);
            inventoryRepository.save(inv);
            setStock(warehouseId, binId, itemCode, quantity);
            log.info("Initialized inventory for {} at {}:{} with quantity {}", itemCode, warehouseId, binId, quantity);
        }
    }

    public void initializeInventory(String itemCode, int quantity) {
        // Dynamic Default Warehouse Lookup
        // 1. Try to find the first active Warehouse
        var warehouses = warehouseRepository.findAll();
        com.app.logistics.inventory.entities.Warehouse defaultWh = warehouses.stream()
                .filter(com.app.logistics.inventory.entities.Warehouse::isActive)
                .findFirst()
                .orElse(null);

        if (defaultWh == null) {
            log.warn("No active warehouse found. Creating 'Default Warehouse'.");
            defaultWh = new com.app.logistics.inventory.entities.Warehouse(null, "Default Warehouse", "000000", "City", true);
            defaultWh = warehouseRepository.save(defaultWh);
        }

        // 2. Try to find the first active bin in that warehouse
        var bins = binRepository.findByWarehouseId(defaultWh.getId());
        com.app.logistics.inventory.entities.Bin defaultBin = bins.stream()
                .filter(com.app.logistics.inventory.entities.Bin::isActive)
                .findFirst()
                .orElse(null);

        if (defaultBin == null) {
            log.warn("No active bin found in Default Warehouse. Creating 'Default Bin'.");
            defaultBin = new com.app.logistics.inventory.entities.Bin(null, "A-01-01", defaultWh, "General", true);
            defaultBin = binRepository.save(defaultBin);
        }
        
        log.info("Initializing inventory for {} at {} (ID: {}): {} (ID: {})", 
                itemCode, defaultWh.getName(), defaultWh.getId(), defaultBin.getBinCode(), defaultBin.getId());

        initializeInventory(defaultWh.getId(), defaultBin.getId(), itemCode, quantity);
    }

    @Transactional
    public void updateInventoryStock(Long warehouseId, Long binId, String itemCode, int newQuantity) {
        InventoryId id = new InventoryId(itemCode, warehouseId, binId);
        inventoryRepository.findById(id).ifPresentOrElse(inv -> {
            int oldQty = inv.getQuantity();
            inv.setQuantity(newQuantity);
            inventoryRepository.save(inv);
            
            // Redis Sync
            setStock(warehouseId, binId, itemCode, newQuantity);
            
            // Audit Log
            InventoryTransaction tx = new InventoryTransaction();
            tx.setItemCode(itemCode);
            tx.setWarehouseId(warehouseId);
            tx.setBinId(binId);
            tx.setQuantityChange(newQuantity - oldQty);
            tx.setType(InventoryTransaction.TransactionType.MANUAL_ADJUSTMENT);
            tx.setReason("Product Update Sync");
            transactionRepository.save(tx);
            
            log.info("Synced stock update for {} at {}:{}: {} -> {}", itemCode, warehouseId, binId, oldQty, newQuantity);
        }, () -> {
            log.warn("Item {} not found in Inventory DB at {}:{} during sync. Initializing now.", itemCode, warehouseId, binId);
            initializeInventory(warehouseId, binId, itemCode, newQuantity);
        });
    }

    @Transactional
    public void updateInventoryStock(String itemCode, int newQuantity) {
        // Find where the item lives
        var inventories = inventoryRepository.findByItemCode(itemCode);
        if (!inventories.isEmpty()) {
            Inventory target = inventories.get(0);
            updateInventoryStock(target.getWarehouseId(), target.getBinId(), itemCode, newQuantity);
        } else {
            log.warn("Item {} not found for update. Initializing at default location.", itemCode);
            // This will now use the dynamic lookup logic we just implemented
            initializeInventory(itemCode, newQuantity);
        }
    }

    public void loadStockFromDB(Long warehouseId, Long binId, String itemCode) {
        InventoryId id = new InventoryId(itemCode, warehouseId, binId);
        Optional<Inventory> inventory = inventoryRepository.findById(id);
        if (inventory.isPresent()) {
            setStock(warehouseId, binId, itemCode, inventory.get().getQuantity());
            log.info("Loaded stock for {} at {}:{} from DB: {}", itemCode, warehouseId, binId, inventory.get().getQuantity());
        } else {
            log.error("Item {} not found in Inventory DB at {}:{}. Setting Redis stock to 0.", itemCode, warehouseId, binId);
            setStock(warehouseId, binId, itemCode, 0);
        }
    }

    private static final String RESERVATION_EXPIRY_SET = "inventory:reservations:expiry";

    /**
     * Recovery script: Atomically increments stock and removes from expiry set.
     */
    private static final String RECOVER_SCRIPT = "if redis.call('zrem', KEYS[2], ARGV[2]) > 0 then " +
            "   return redis.call('incrby', KEYS[1], ARGV[1]); " +
            "else " +
            "   return 0; " +
            "end;";

    /**
     * Atomically reserves a batch of items. All or nothing.
     */
    public boolean reserveBatch(java.util.List<com.app.logistics.inventory.payloads.InventoryRequest> requests, String lockId) {
        if (requests.isEmpty()) return true;

        List<String> keys = requests.stream()
                .map(r -> INVENTORY_KEY_PREFIX + r.warehouseId() + ":" + r.binId() + ":" + r.itemCode())
                .toList();

        String[] values = requests.stream()
                .map(r -> String.valueOf(r.quantity()))
                .toArray(String[]::new);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(BATCH_RESERVE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, keys, (Object[]) values);

        if (result != null && result == 1) {
            long expiresAt = System.currentTimeMillis() + Duration.ofMinutes(15).toMillis();
            for (var r : requests) {
                String resValue = r.warehouseId() + ":" + r.binId() + ":" + r.itemCode() + ":" + lockId + ":" + r.quantity();
                redisTemplate.opsForZSet().add(RESERVATION_EXPIRY_SET, resValue, expiresAt);
            }
            log.info("Batch reservation successful for {} items. Lock ID: {}", requests.size(), lockId);
            return true;
        }

        log.warn("Batch reservation failed. One or more items out of stock.");
        return false;
    }

    /**
     * Atomically reserves flash sale inventory.
     */
    public boolean reserveFlash(Long productId, int quantity, String lockId) {
        String tenantId = com.app.core.multitenancy.TenantContext.getTenantId();
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(FLASH_RESERVE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.emptyList(), tenantId, String.valueOf(productId), String.valueOf(quantity));

        if (result != null && result > 0) {
            long expiresAt = System.currentTimeMillis() + Duration.ofMinutes(5).toMillis(); // Flash sales expire faster
            String resValue = "FLASH:" + productId + ":" + lockId + ":" + quantity;
            redisTemplate.opsForZSet().add(RESERVATION_EXPIRY_SET, resValue, expiresAt);
            return true;
        }
        return false;
    }

    // 2. Atomic Reservation
    public boolean reserveStock(Long warehouseId, Long binId, String itemCode, int quantity) {
        return reserveStockWithRetry(warehouseId, binId, itemCode, quantity, java.util.UUID.randomUUID().toString(), 0);
    }

    public boolean reserveStock(Long warehouseId, Long binId, String itemCode, int quantity, String lockId) {
        return reserveStockWithRetry(warehouseId, binId, itemCode, quantity, lockId, 0);
    }

    private boolean reserveStockWithRetry(Long warehouseId, Long binId, String itemCode, int quantity, String lockId, int retryCount) {
        String redisKey = INVENTORY_KEY_PREFIX + warehouseId + ":" + binId + ":" + itemCode;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RESERVE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(redisKey), String.valueOf(quantity));

        if (result == -1) {
            if (retryCount >= 1) {
                log.error("Failed to reserve stock for {} at {}:{} after read-through.", itemCode, warehouseId, binId);
                return false;
            }

            log.warn("Stock not initialized in Redis for {} at {}:{}, initiating Read-Through", itemCode, warehouseId, binId);
            try {
                loadStockFromDB(warehouseId, binId, itemCode);
                return reserveStockWithRetry(warehouseId, binId, itemCode, quantity, lockId, retryCount + 1);
            } catch (Exception e) {
                log.error("Read-Through failed for {}: {}", itemCode, e.getMessage());
                return false;
            }
        } else if (result == -2) {
            log.info("Insufficient stock for reservation: {} at {}:{}", itemCode, warehouseId, binId);
            return false;
        } else {
            log.info("Stock reserved for {}. Remaining in Redis: {}", itemCode, result);

            // Track reservation in a Sorted Set for auto-recovery
            long expiresAt = System.currentTimeMillis() + Duration.ofMinutes(15).toMillis();
            String resValue = warehouseId + ":" + binId + ":" + itemCode + ":" + (lockId != null ? lockId : java.util.UUID.randomUUID().toString()) + ":" + quantity;
            redisTemplate.opsForZSet().add(RESERVATION_EXPIRY_SET, resValue, expiresAt);

            return true;
        }
    }

    /**
     * Cleanup worker polled by a scheduler to release stock for abandoned carts.
     */
    public void releaseExpiredReservations() {
        long now = System.currentTimeMillis();
        var expired = redisTemplate.opsForZSet().rangeByScore(RESERVATION_EXPIRY_SET, 0, now);

        if (expired == null || expired.isEmpty())
            return;

        log.info("Found {} expired reservations to recover", expired.size());

        for (String entry : expired) {
            try {
                String[] parts = entry.split(":");
                Long warehouseId = Long.parseLong(parts[0]);
                Long binId = Long.parseLong(parts[1]);
                String itemCode = parts[2];
                int quantity = Integer.parseInt(parts[4]);

                String redisKey = INVENTORY_KEY_PREFIX + warehouseId + ":" + binId + ":" + itemCode;
                DefaultRedisScript<Long> recoverScript = new DefaultRedisScript<>(RECOVER_SCRIPT, Long.class);
                redisTemplate.execute(recoverScript,
                        java.util.List.of(redisKey, RESERVATION_EXPIRY_SET),
                        String.valueOf(quantity), entry);

                log.info("Successfully recovered {} units for item {} at {}:{}", quantity, itemCode, warehouseId, binId);
            } catch (Exception e) {
                log.error("Failed to recover expired reservation entry {}: {}", entry, e.getMessage());
            }
        }
    }

    // 3. Confirm (Success) - NOW PERSISTS TO DB
    @Transactional
    public void confirmStock(Long warehouseId, Long binId, String itemCode, int quantity, String lockId) {
        if (lockId == null) {
            log.warn("Cannot confirm stock for {} without lockId", itemCode);
            return;
        }
        String member = warehouseId + ":" + binId + ":" + itemCode + ":" + lockId + ":" + quantity;
        
        // Remove from Redis reservation tracking
        Long removed = redisTemplate.opsForZSet().remove(RESERVATION_EXPIRY_SET, member);
        
        if (removed != null && removed > 0) {
            log.info("Redis Reservation {} confirmed at {}:{}. Persisting to DB...", lockId, warehouseId, binId);
            
            // Persist to DB
            InventoryId id = new InventoryId(itemCode, warehouseId, binId);
            inventoryRepository.findById(id).ifPresentOrElse(inv -> {
                inv.setQuantity(inv.getQuantity() - quantity);
                inventoryRepository.save(inv);
                
                // Audit Log
                InventoryTransaction tx = new InventoryTransaction();
                tx.setItemCode(itemCode);
                tx.setWarehouseId(warehouseId);
                tx.setBinId(binId);
                tx.setQuantityChange(-quantity);
                tx.setType(InventoryTransaction.TransactionType.OUTBOUND_ORDER);
                tx.setReferenceId(lockId);
                tx.setReason("Order Confirmed");
                transactionRepository.save(tx);
                
                log.info("Stock permanently deducted for {} in DB. New Quantity: {}", itemCode, inv.getQuantity());
            }, () -> log.error("CRITICAL: Inventory missing in DB for item {} at {}:{} during confirmation!", itemCode, warehouseId, binId));
            
        } else {
            log.warn("Reservation {} for item {} not found or already expired.", lockId, itemCode);
        }
    }

    @Transactional
    public void confirmFlash(Long productId, int quantity, String lockId) {
        String member = "FLASH:" + productId + ":" + lockId + ":" + quantity;
        Long removed = redisTemplate.opsForZSet().remove(RESERVATION_EXPIRY_SET, member);

        if (removed != null && removed > 0) {
            log.info("Flash Inventory confirmed for product {}. Applying to DB...", productId);

            // Sync to main DB (deduct from total quantity)
            var inventories = inventoryRepository.findByItemCode("PROD-" + productId);
            if (inventories.isEmpty()) {
                // Fallback: try finding by just productId pattern if PROD- prefix is missing in some records
                inventories = inventoryRepository.findByItemCode(String.valueOf(productId));
            }

            inventories.stream().findFirst().ifPresent(inv -> {
                inv.setQuantity(inv.getQuantity() - quantity);
                inventoryRepository.save(inv);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setItemCode(inv.getItemCode());
                tx.setWarehouseId(inv.getWarehouseId());
                tx.setBinId(inv.getBinId());
                tx.setQuantityChange(-quantity);
                tx.setType(InventoryTransaction.TransactionType.OUTBOUND_ORDER);
                tx.setReason("Flash Sale Order Confirmed");
                transactionRepository.save(tx);
            });
        }
    }

    public void confirmStock(String itemCode, int quantity) {
        // Strategy: find the inventory first, then confirm.
        var inventories = inventoryRepository.findByItemCode(itemCode);
        if (!inventories.isEmpty()) {
            Inventory target = inventories.get(0);
            // using "AUTO-CONFIRM" as lockId since none was provided
            confirmStock(target.getWarehouseId(), target.getBinId(), itemCode, quantity, "AUTO-CONFIRM-" + System.currentTimeMillis());
        } else {
             log.error("Cannot confirm stock for {} - not found in any warehouse.", itemCode);
        }
    }


    @Transactional
    public void receiveProcurementStock(String itemCode, int quantity, String poReference) {
        var inventories = inventoryRepository.findByItemCode(itemCode);
        if (inventories.isEmpty()) {
            log.warn("Item {} not found for PO receipt. Initializing...", itemCode);
            initializeInventory(itemCode, quantity);
            inventories = inventoryRepository.findByItemCode(itemCode);
        }

        if (!inventories.isEmpty()) {
            Inventory target = inventories.get(0);
            int oldQty = target.getQuantity();
            target.setQuantity(oldQty + quantity);
            inventoryRepository.save(target);

            // Sync Redis
            String redisKey = INVENTORY_KEY_PREFIX + target.getWarehouseId() + ":" + target.getBinId() + ":" + itemCode;
            redisTemplate.opsForValue().increment(redisKey, quantity);

            // Audit
            InventoryTransaction tx = new InventoryTransaction();
            tx.setItemCode(itemCode);
            tx.setWarehouseId(target.getWarehouseId());
            tx.setBinId(target.getBinId());
            tx.setQuantityChange(quantity);
            tx.setType(InventoryTransaction.TransactionType.INBOUND_PO);
            tx.setReferenceId(poReference);
            tx.setReason("PO Received: " + poReference);
            transactionRepository.save(tx);

            log.info("Procurement Stock Received for {}: {} units (PO: {}). New level: {}", itemCode, quantity, poReference, target.getQuantity());
        }
    }

    @Transactional
    public void restock(Long warehouseId, Long binId, String itemCode, int quantity, String reason) {
        InventoryId id = new InventoryId(itemCode, warehouseId, binId);
        inventoryRepository.findById(id).ifPresentOrElse(inv -> {
            inv.setQuantity(inv.getQuantity() + quantity);
            inventoryRepository.save(inv);
            
            // Sync Redis
            String redisKey = INVENTORY_KEY_PREFIX + warehouseId + ":" + binId + ":" + itemCode;
            redisTemplate.opsForValue().increment(redisKey, quantity);
            
            // Audit Log
            InventoryTransaction tx = new InventoryTransaction();
            tx.setItemCode(itemCode);
            tx.setWarehouseId(warehouseId);
            tx.setBinId(binId);
            tx.setQuantityChange(quantity);
            tx.setType(InventoryTransaction.TransactionType.RETURN_RESTOCK);
            tx.setReason(reason);
            transactionRepository.save(tx);
            
            log.info("Restocked {} units for item {} at {}:{}. New DB level: {}", quantity, itemCode, warehouseId, binId, inv.getQuantity());
        }, () -> log.error("Item {} not found for restocking at {}:{}", itemCode, warehouseId, binId));
    }

    @Transactional
    public void restock(String itemCode, int quantity, String reason) {
        var inventories = inventoryRepository.findByItemCode(itemCode);
        if (!inventories.isEmpty()) {
            Inventory target = inventories.get(0);
            restock(target.getWarehouseId(), target.getBinId(), itemCode, quantity, reason);
        } else {
            // Use the public method that already has the dynamic default logic
            log.warn("No inventory record found for restocking {}. Initializing at default location.", itemCode);
            initializeInventory(itemCode, quantity);
            
            // We need to fetch it back to log the transaction correctly
            var newInvs = inventoryRepository.findByItemCode(itemCode);
            if (!newInvs.isEmpty()) {
                 Inventory created = newInvs.get(0);
                 InventoryTransaction tx = new InventoryTransaction();
                 tx.setItemCode(itemCode);
                 tx.setWarehouseId(created.getWarehouseId());
                 tx.setBinId(created.getBinId());
                 tx.setQuantityChange(quantity);
                 tx.setType(InventoryTransaction.TransactionType.RETURN_RESTOCK);
                 tx.setReason(reason + " (Initialized)");
                 transactionRepository.save(tx);
            }
        }
    }

    // 4. Rollback (if payment fails)
    public void releaseStock(String itemCode, int quantity) {
        // Since we don't have the specific warehouse/bin from the cancellation event without looking up the order details (which we don't have here),
        // we'll restock to the default/primary location for that item.
        restock(itemCode, quantity, "Order Cancellation Release (Restored to stock)");
    }

    // 4. Check Stock (Peek) - For Cart operations
    public boolean checkStock(Long warehouseId, Long binId, String itemCode, int quantity) {
        String redisKey = INVENTORY_KEY_PREFIX + warehouseId + ":" + binId + ":" + itemCode;
        String stockStr = redisTemplate.opsForValue().get(redisKey);
        if (stockStr == null) {
            try {
                loadStockFromDB(warehouseId, binId, itemCode);
                stockStr = redisTemplate.opsForValue().get(redisKey);
                if(stockStr == null) return false; 
            } catch (Exception e) {
                log.error("Stock check failed for {} at {}:{}: {}", itemCode, warehouseId, binId, e.getMessage());
                return false;
            }
        }

        try {
            long stock = Long.parseLong(stockStr);
            return stock >= quantity;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int getAggregateStock(String itemCode) {
        return inventoryRepository.findByItemCode(itemCode).stream()
                .mapToInt(Inventory::getQuantity)
                .sum();
    }

    @Transactional
    public void adjustStock(String itemCode, int newQuantity, String batchNumber, java.time.LocalDate expiryDate, String reason) {
        var inventories = inventoryRepository.findByItemCode(itemCode);
        if (!inventories.isEmpty()) {
            Inventory target = inventories.get(0);
            int oldQty = target.getQuantity();
            target.setQuantity(newQuantity);
            inventoryRepository.save(target);

            // Redis Sync
            setStock(target.getWarehouseId(), target.getBinId(), itemCode, newQuantity);

            // Audit
            InventoryTransaction tx = new InventoryTransaction();
            tx.setItemCode(itemCode);
            tx.setWarehouseId(target.getWarehouseId());
            tx.setBinId(target.getBinId());
            tx.setQuantityChange(newQuantity - oldQty);
            tx.setType(InventoryTransaction.TransactionType.MANUAL_ADJUSTMENT);
            tx.setReason(reason != null ? reason : "Admin Manual Adjustment");
            tx.setBatchNumber(batchNumber);
            tx.setExpiryDate(expiryDate);
            transactionRepository.save(tx);
            
            log.info("Admin adjusted stock for {} at {}:{}: {} -> {} (Batch: {})", itemCode, target.getWarehouseId(), target.getBinId(), oldQty, newQuantity, batchNumber);
        } else {
             // Handle initialization if needed
             log.warn("Item {} not found for adjustment. Initializing...", itemCode);
             initializeInventory(itemCode, newQuantity);
        }
    }

    public boolean checkAggregateStock(String itemCode, int quantity) {
        return getAggregateStock(itemCode) >= quantity;
    }

    public boolean checkStock(String itemCode, int quantity) {
        // Simple aggregate check for now, can be optimized later
        return getAggregateStock(itemCode) >= quantity;
    }
}
