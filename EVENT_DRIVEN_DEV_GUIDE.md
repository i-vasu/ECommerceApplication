# Event-Driven Development Guide

## Quick Reference for Developers

### When to Use Events vs Direct Calls

#### ✅ Use Events When:
- Cross-module communication
- Async processing is acceptable
- Multiple modules need to react to same action
- You want loose coupling
- Action is a "notification" of something that happened

#### ❌ Use Direct Calls When:
- Same module communication
- Synchronous response needed immediately
- Transaction boundary must be maintained
- Simple utility/helper functions
- Performance-critical path

---

## How to Publish an Event

### Step 1: Define the Event (in modulith-kernel)

```java
package com.app.core.events;

/**
 * Published when a product is created.
 * Listeners: Discovery (indexing), Marketing (campaigns), Analytics
 */
public record ProductCreatedEvent(
    Long productId,
    String itemCode,
    String productName,
    BigDecimal price,
    Integer quantity
) {}
```

### Step 2: Publish the Event

```java
@Service
public class ProductServiceImpl implements ProductService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    public ProductDTO addProduct(Long categoryId, Product product) {
        // ... business logic ...
        
        var savedProduct = productRepo.save(product);
        
        // Publish event
        eventPublisher.publishEvent(new ProductCreatedEvent(
            savedProduct.getProductId(),
            savedProduct.getItemCode(),
            savedProduct.getProductName(),
            savedProduct.getSpecialPrice(),
            savedProduct.getQuantity()
        ));
        
        return productMapper.productToProductDTO(savedProduct);
    }
}
```

---

## How to Listen to an Event

### Step 1: Create Event Listener

```java
@Component
public class ProductEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);
    
    private final SearchService searchService;
    
    public ProductEventListener(SearchService searchService) {
        this.searchService = searchService;
    }
    
    /**
     * Async listener - runs in separate thread pool
     * Use for non-critical operations that can fail independently
     */
    @Async
    @EventListener
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("Indexing new product: {}", event.productId());
        try {
            searchService.indexProduct(event.productId());
        } catch (Exception e) {
            log.error("Failed to index product: {}", event.productId(), e);
            // Don't throw - async listeners shouldn't affect main flow
        }
    }
    
    /**
     * Sync listener - runs in same transaction
     * Use for critical operations that must succeed
     */
    @EventListener
    @Transactional
    public void handleProductDeleted(ProductDeletedEvent event) {
        log.info("Removing product from cache: {}", event.productId());
        cacheService.evict("product", event.productId());
    }
}
```

### Step 2: Enable Async Processing (if using @Async)

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.initialize();
        return executor;
    }
}
```

---

## Event Naming Conventions

### Pattern: `{Entity}{Action}Event`

- **ProductCreatedEvent** - Product was created
- **OrderPlacedEvent** - Order was placed
- **ShipmentDeliveredEvent** - Shipment was delivered
- **UserRegisteredEvent** - User registered
- **PaymentCompletedEvent** - Payment completed

### Event Types:

1. **Domain Events** - Business actions (OrderPlacedEvent)
2. **Integration Events** - Cross-system (ERPSyncCompletedEvent)
3. **Activity Events** - User actions (ProductViewedEvent)
4. **State Events** - State changes (OrderStatusChangedEvent)

---

## Best Practices

### 1. **Events are Immutable**
```java
// ✅ Good - Record (immutable)
public record ProductCreatedEvent(Long productId, String name) {}

// ❌ Bad - Mutable class
public class ProductCreatedEvent {
    private Long productId;
    public void setProductId(Long id) { this.productId = id; }
}
```

### 2. **Events Should Be Self-Contained**
```java
// ✅ Good - Contains all necessary data
public record OrderPlacedEvent(
    Long orderId,
    Long userId,
    BigDecimal totalAmount,
    List<OrderItemDTO> items,
    ShippingAddress address
) {}

// ❌ Bad - Requires additional lookups
public record OrderPlacedEvent(Long orderId) {}
```

### 3. **Use Async for Non-Critical Operations**
```java
// ✅ Good - Search indexing can fail without affecting order
@Async
@EventListener
public void indexProduct(ProductCreatedEvent event) {
    searchService.index(event);
}

// ❌ Bad - Payment processing must be synchronous
@Async  // DON'T DO THIS
@EventListener
public void processPayment(OrderPlacedEvent event) {
    paymentService.charge(event);
}
```

### 4. **Handle Failures Gracefully**
```java
@Async
@EventListener
public void sendEmail(OrderPlacedEvent event) {
    try {
        emailService.sendConfirmation(event.userId());
    } catch (Exception e) {
        log.error("Failed to send email for order: {}", event.orderId(), e);
        // Consider: Publish EmailFailedEvent for retry logic
        // DON'T: throw exception (will be swallowed in async)
    }
}
```

### 5. **Document Event Contracts**
```java
/**
 * Published when a product is created in the catalog.
 * 
 * Listeners:
 * - Discovery Module: Indexes product for search
 * - Marketing Module: Creates promotional campaigns
 * - Analytics Module: Tracks product metrics
 * 
 * @param productId Unique identifier of the product
 * @param itemCode ERP item code
 * @param productName Display name
 * @param price Current price
 * @param quantity Available quantity
 */
public record ProductCreatedEvent(
    Long productId,
    String itemCode,
    String productName,
    BigDecimal price,
    Integer quantity
) {}
```

---

## Common Patterns

### Pattern 1: Event Sourcing (Future)
```java
// Store events as source of truth
@EventListener
public void storeEvent(DomainEvent event) {
    eventStore.save(event);
}
```

### Pattern 2: Saga Pattern (Future)
```java
// Coordinate distributed transactions
@EventListener
public void handleOrderPlaced(OrderPlacedEvent event) {
    // Step 1: Reserve inventory
    // Step 2: Process payment
    // Step 3: Create shipment
    // If any step fails, publish compensation events
}
```

### Pattern 3: Outbox Pattern (Future)
```java
// Ensure event delivery with database
@Transactional
public void createOrder(Order order) {
    orderRepo.save(order);
    outboxRepo.save(new OutboxEvent("OrderPlacedEvent", order));
    // Background job publishes from outbox
}
```

---

## Testing Events

### Test Event Publishing

```java
@SpringBootTest
class ProductServiceTest {
    
    @MockBean
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private ProductService productService;
    
    @Test
    void shouldPublishEventWhenProductCreated() {
        // Given
        Product product = new Product();
        product.setProductName("Test Product");
        
        // When
        productService.addProduct(1L, product);
        
        // Then
        verify(eventPublisher).publishEvent(argThat(event -> 
            event instanceof ProductCreatedEvent &&
            ((ProductCreatedEvent) event).productName().equals("Test Product")
        ));
    }
}
```

### Test Event Listening

```java
@SpringBootTest
class ProductEventListenerTest {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @MockBean
    private SearchService searchService;
    
    @Test
    void shouldIndexProductWhenCreated() {
        // Given
        ProductCreatedEvent event = new ProductCreatedEvent(
            1L, "ITEM-001", "Test Product", BigDecimal.TEN, 100
        );
        
        // When
        eventPublisher.publishEvent(event);
        
        // Then
        await().atMost(2, SECONDS).untilAsserted(() ->
            verify(searchService).indexProduct(1L)
        );
    }
}
```

---

## Troubleshooting

### Event Not Being Received

1. **Check @EventListener annotation**
   ```java
   @EventListener  // Make sure this is present
   public void handleEvent(MyEvent event) { }
   ```

2. **Check component scanning**
   ```java
   @Component  // Listener class must be a Spring bean
   public class MyEventListener { }
   ```

3. **Check event type matches exactly**
   ```java
   // Publisher
   eventPublisher.publishEvent(new ProductCreatedEvent(...));
   
   // Listener - must match exact type
   @EventListener
   public void handle(ProductCreatedEvent event) { }  // ✅
   public void handle(Object event) { }  // ❌ Won't receive typed events
   ```

### Async Events Not Working

1. **Enable @EnableAsync**
   ```java
   @Configuration
   @EnableAsync
   public class AsyncConfig { }
   ```

2. **Configure executor**
   ```java
   @Bean
   public Executor taskExecutor() {
       return new ThreadPoolTaskExecutor();
   }
   ```

3. **Check @Async is on public method**
   ```java
   @Async
   public void handleEvent() { }  // ✅
   
   @Async
   private void handleEvent() { }  // ❌ Won't work
   ```

---

## Migration Checklist

When converting from direct calls to events:

- [ ] Define event record in modulith-kernel
- [ ] Document event contract (who publishes, who listens)
- [ ] Update publisher to use eventPublisher.publishEvent()
- [ ] Create event listener in consuming module
- [ ] Add error handling in listener
- [ ] Add tests for publishing and listening
- [ ] Remove direct service dependency
- [ ] Update documentation
- [ ] Monitor event flow in production

---

**Last Updated**: 2026-01-29T03:17:00Z
