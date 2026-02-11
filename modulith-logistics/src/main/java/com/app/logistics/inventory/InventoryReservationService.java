package com.app.logistics.inventory;

import com.app.logistics.inventory.entities.Inventory;
import com.app.logistics.inventory.entities.InventoryTransaction;
import com.app.logistics.inventory.repositories.InventoryRepository;
import com.app.logistics.inventory.repositories.InventoryTransactionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Service
@io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "inventory")
@Retryable(retryFor = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public class InventoryReservationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryReservationService.class);

    private final StringRedisTemplate redisTemplate;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    public InventoryReservationService(StringRedisTemplate redisTemplate,
                                       InventoryRepository inventoryRepository,
                                       InventoryTransactionRepository transactionRepository) {
        this.redisTemplate = redisTemplate;
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
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

    // 1. Initialize Stock in Redis (Read-Through from DB)
    public void setStock(String itemCode, int quantity) {
        redisTemplate.opsForValue().set(INVENTORY_KEY_PREFIX + itemCode, String.valueOf(quantity),
                Duration.ofHours(24));
    }

    public void initializeInventory(String itemCode, int quantity) {
        if (!inventoryRepository.findByItemCode(itemCode).isPresent()) {
            Inventory inv = new Inventory(itemCode, quantity, 0, "MAIN", 0L);
            inventoryRepository.save(inv);
            setStock(itemCode, quantity);
            log.info("Initialized inventory for {} with quantity {}", itemCode, quantity);
        }
    }

    @Transactional
    public void updateInventoryStock(String itemCode, int newQuantity) {
        inventoryRepository.findByItemCode(itemCode).ifPresentOrElse(inv -> {
            int oldQty = inv.getQuantity();
            inv.setQuantity(newQuantity);
            inventoryRepository.save(inv);
            
            // Redis Sync
            setStock(itemCode, newQuantity);
            
            // Audit Log
            InventoryTransaction tx = new InventoryTransaction();
            tx.setItemCode(itemCode);
            tx.setQuantityChange(newQuantity - oldQty);
            tx.setType(InventoryTransaction.TransactionType.MANUAL_ADJUSTMENT);
            tx.setReason("Product Update Sync");
            transactionRepository.save(tx);
            
            log.info("Synced stock update for {}: {} -> {}", itemCode, oldQty, newQuantity);
        }, () -> {
            log.warn("Item {} not found in Inventory DB during sync. Initializing now.", itemCode);
            initializeInventory(itemCode, newQuantity);
        });
    }

    public void loadStockFromDB(String itemCode) {
        Optional<Inventory> inventory = inventoryRepository.findByItemCode(itemCode);
        if (inventory.isPresent()) {
            setStock(itemCode, inventory.get().getQuantity());
            log.info("Loaded stock for {} from DB: {}", itemCode, inventory.get().getQuantity());
        } else {
            log.error("Item {} not found in Inventory DB. Setting Redis stock to 0.", itemCode);
            setStock(itemCode, 0);
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

    // 2. Atomic Reservation
    public boolean reserveStock(String itemCode, int quantity) {
        return reserveStockWithRetry(itemCode, quantity, java.util.UUID.randomUUID().toString(), 0);
    }

    public boolean reserveStock(String itemCode, int quantity, String lockId) {
        return reserveStockWithRetry(itemCode, quantity, lockId, 0);
    }

    private boolean reserveStockWithRetry(String itemCode, int quantity, String lockId, int retryCount) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RESERVE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(INVENTORY_KEY_PREFIX + itemCode),
                String.valueOf(quantity));

        if (result == -1) {
            if (retryCount >= 1) {
                log.error("Failed to reserve stock for {} after read-through. Potential Redis issue.", itemCode);
                return false;
            }

            log.warn("Stock not initialized in Redis for {}, initiating Read-Through from DB", itemCode);
            try {
                loadStockFromDB(itemCode);
                return reserveStockWithRetry(itemCode, quantity, lockId, retryCount + 1);
            } catch (Exception e) {
                log.error("Read-Through failed for {}: {}", itemCode, e.getMessage());
                return false;
            }
        } else if (result == -2) {
            log.info("Insufficient stock for reservation: {}", itemCode);
            return false;
        } else {
            log.info("Stock reserved for {}. Remaining in Redis: {}", itemCode, result);

            // Track reservation in a Sorted Set for auto-recovery
            long expiresAt = System.currentTimeMillis() + Duration.ofMinutes(15).toMillis();
            String resValue = itemCode + ":" + (lockId != null ? lockId : java.util.UUID.randomUUID().toString()) + ":"
                    + quantity;
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
                String itemCode = parts[0];
                int quantity = Integer.parseInt(parts[2]);

                DefaultRedisScript<Long> recoverScript = new DefaultRedisScript<>(RECOVER_SCRIPT, Long.class);
                redisTemplate.execute(recoverScript,
                        java.util.List.of(INVENTORY_KEY_PREFIX + itemCode, RESERVATION_EXPIRY_SET),
                        String.valueOf(quantity), entry);

                log.info("Successfully recovered {} units for item {}", quantity, itemCode);
            } catch (Exception e) {
                log.error("Failed to recover expired reservation entry {}: {}", entry, e.getMessage());
            }
        }
    }

    // 3. Confirm (Success) - NOW PERSISTS TO DB
    @Transactional
    public void confirmStock(String itemCode, int quantity, String lockId) {
        if (lockId == null) {
            log.warn("Cannot confirm stock for {} without lockId", itemCode);
            return;
        }
        String member = itemCode + ":" + lockId + ":" + quantity;
        
        // Remove from Redis reservation tracking
        Long removed = redisTemplate.opsForZSet().remove(RESERVATION_EXPIRY_SET, member);
        
        if (removed != null && removed > 0) {
            log.info("Redis Reservation {} confirmed. Persisting to DB...", lockId);
            
            // Persist to DB
            inventoryRepository.findByItemCode(itemCode).ifPresentOrElse(inv -> {
                inv.setQuantity(inv.getQuantity() - quantity);
                inventoryRepository.save(inv);
                
                // Audit Log
                InventoryTransaction tx = new InventoryTransaction();
                tx.setItemCode(itemCode);
                tx.setQuantityChange(-quantity);
                tx.setType(InventoryTransaction.TransactionType.OUTBOUND_ORDER);
                tx.setReferenceId(lockId); // Using LockID/OrderID
                tx.setReason("Order Confirmed");
                transactionRepository.save(tx);
                
                log.info("Stock permanently deducted for {} in DB. New Quantity: {}", itemCode, inv.getQuantity());
            }, () -> log.error("CRITICAL: Inventory missing in DB for item {} during confirmation!", itemCode));
            
        } else {
            log.warn("Reservation {} for item {} not found or already expired.", lockId, itemCode);
        }
    }

    public void confirmStock(String itemCode, int quantity) {
        confirmStock(itemCode, quantity, null);
    }

    // 4. Rollback (if payment fails)
    public void releaseStock(String itemCode, int quantity) {
        redisTemplate.opsForValue().increment(INVENTORY_KEY_PREFIX + itemCode, quantity);
        log.info("Stock released/restored for {}", itemCode);
    }

    @Transactional
    public void restock(String itemCode, int quantity, String reason) {
        inventoryRepository.findByItemCode(itemCode).ifPresentOrElse(inv -> {
            inv.setQuantity(inv.getQuantity() + quantity);
            inventoryRepository.save(inv);
            
            // Sync Redis
            redisTemplate.opsForValue().increment(INVENTORY_KEY_PREFIX + itemCode, quantity);
            
            // Audit Log
            InventoryTransaction tx = new InventoryTransaction();
            tx.setItemCode(itemCode);
            tx.setQuantityChange(quantity);
            tx.setType(InventoryTransaction.TransactionType.RETURN_RESTOCK);
            tx.setReason(reason);
            transactionRepository.save(tx);
            
            log.info("Restocked {} units for item {}. New DB level: {}", quantity, itemCode, inv.getQuantity());
        }, () -> log.error("Item {} not found for restocking", itemCode));
    }

    // 4. Check Stock (Peek) - For Cart operations
    public boolean checkStock(String itemCode, int quantity) {
        String stockStr = redisTemplate.opsForValue().get(INVENTORY_KEY_PREFIX + itemCode);
        if (stockStr == null) {
            try {
                loadStockFromDB(itemCode);
                stockStr = redisTemplate.opsForValue().get(INVENTORY_KEY_PREFIX + itemCode);
                // Double check after load
                if(stockStr == null) return false; 
            } catch (Exception e) {
                log.error("Stock check failed for {}: {}", itemCode, e.getMessage());
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
}
