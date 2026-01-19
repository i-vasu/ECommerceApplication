---
description: Strategy to prevent and reduce memory leaks in the application
---

# Memory Leak Prevention Strategy

## 1. Streaming Data Processing
**Problem:** Loading large files (images, datasets) entirely into memory (`byte[]` or `List`) causes heap spikes and GC pressure.
**Solution:** Use `InputStream` and streaming callbacks.

### Implementation Example (`ProductDataFlowService.java`)
Instead of `restTemplate.getForObject(url, byte[].class)`:
```java
restTemplate.execute(url, HttpMethod.GET, null, response -> {
    try (InputStream is = response.getBody()) {
        // Process stream directly (e.g., generate embedding, save to file)
        processStream(is);
    }
    return null;
});
```

## 2. Resource Management with Try-With-Resources
**Problem:** Unclosed `InputStreams`, DB cursors, or native arenas lead to leaks.
**Solution:** Always use try-with-resources.

### Implementation Example (`ImageService.java`)
```java
// Ensure passed streams are closed if ownership is transferred
try (InputStream is = imageStream) {
    BufferedImage image = ImageIO.read(is);
    // ...
}
```

## 3. Native Memory Management (Vips-FFM)
**Problem:** Native libraries allocate off-heap memory that GC doesn't track well.
**Solution:**
- Use `Arena` or `Scope` patterns (e.g., `Vips.run(arena -> ...)`).
- Explicitly delete temporary files in `finally` blocks.
- Consider disabling internal caches for low-memory environments.

## 4. Virtual Thread Hygiene
**Problem:** Creating unlimited virtual threads for massive tasks can exhaust heap (each thread has stack metadata).
**Solution:**
- Use `Executors.newVirtualThreadPerTaskExecutor()` within a try-with-resource block to scope lifetime.
- For massive lists, use bounded concurrency or semaphores, even with virtual threads, to limit in-flight request memory overhead.

## 5. Pagination
**Problem:** Fetching "All" records (e.g., `fetchAllItemCodes`) scales poorly.
**Solution:** Implement cursor-based or offset-based pagination.
- **Do not** load 100k IDs into a List.
- Process pages of 1000 items at a time.

## 6. Caching Configuration
- **Redis:** Ensure `entryTtl` is set.
- **Local Cache:** Avoid unbound `ConcurrentHashMap`. Use Caffeine with `maximumSize`.

## 7. Monitoring
- Enable Native Memory Tracking: `-XX:NativeMemoryTracking=summary`
- Use ZGC for large heaps: `-XX:+UseZGC`
