# The Definitive List: 100 Unique Architectural Pitfalls
This list is tailored for a high-performance E-commerce stack using **Spring Boot, ERPNext, DragonflyDB, and ParadeDB**.
---
## 🏗️ I. High-Level Architecture
1.  **The Death Star Dependency:** Services calling each other in a synchronous chain (A->B->C->D). If D is slow, the whole site is down.
2.  **Shared Database Schema:** Multiple services writing to the same table, making schema changes impossible without breaking everything.
3.  **The Distributed Monolith:** Microservices that are so tightly coupled they must be deployed together.
4.  **Database as a Queue:** Using a standard SQL table for task queuing instead of DragonflyDB, leading to table locking under load.
5.  **Architecture by Hype:** Using a tool (like Kubernetes or Kafka) just because it's popular, not because the business needs the complexity.
6.  **Ignoring Latency Budgets:** Not defining an upper limit for internal API calls (e.g., "All internal calls must return <100ms").
7.  **Lack of ADRs:** Not documenting *why* a decision was made, leading to "Tribal Knowledge" rot.
8.  **Circular Service Dependencies:** Service A depends on B, which depends on A.
9.  **Everything is a Service:** Creating "Nanorevices" (e.g., a "Tax Calculation Service") that add 50ms of network overhead for 1ms of work.
10. **The God Service:** One microservice (usually "Ordering") that contains 80% of the company's code.
## 🧩 II. Domain-Driven Design (DDD)
11. **The Anemic Domain Model:** Entities are just getters/setters; all logic is in "Service" classes.
12. **The God Aggregate:** An `Order` object that contains all history, tracking, payments, and reviews, making it 50MB in memory.
13. **Context Leakage:** Using the ERPNext "Item" model directly in your Spring Boot "Catalog" service.
14. **Bypassing the Aggregate Root:** Modifying an `OrderItem` directly instead of going through the `Order` root.
15. **Ubiquitous Language Gap:** Developers call it "User," Business calls it "Customer," and Database calls it "Account."
16. **Artificial Aggregates:** Grouping things together that don't have a shared lifecycle just to "reduce service count."
17. **DTO-Entity Confusion:** Using the same Java class for your Database, your internal Logic, and your JSON Response.
18. **Ignoring Value Objects:** Representing "Price" as a `double` instead of a `Money` value object with currency logic.
19. **Massive Repositories:** Repository methods like `findAllByStatus()` that return 1 million records unknowingly.
20. **Transactional Boundary Overreach:** Trying to keep an Order, Payment, and Shipping record in a single ACID transaction across services.
## 📡 III. Event-Driven Architecture (EDA)
21. **Fat Events:** Including the entire User object in an "OrderPlaced" event instead of just the `userId`.
22. **Chatty Events:** Emitting an event for every single minor state change (e.g., "OrderCursorMoved").
23. **Lack of Event Versioning:** Changing an event schema and breaking all consumers currently in production.
24. **The Event Loop:** A -> Event -> B -> Event -> A.
25. **Non-Idempotent Consumers:** Charging a credit card twice because the "PaymentProcessed" event was delivered twice.
26. **Ignoring DLQs:** Having a Dead Letter Queue but never setting up an alert when it fills up.
27. **Shared Event Libraries:** Consumer and Producer sharing a Java JAR for event classes, creating a "Binary Coupling."
28. **Order Dependency:** Assuming Event B will always arrive after Event A.
29. **Fire and Forget (Literally):** Sending an event to DragonflyDB without checking if the stream actually accepted it.
30. **Big Event Blobs:** Sending 5MB images as base64 inside an event instead of a URL.
## ☕ IV. Spring Boot / JVM Specific
31. **ThreadLocal Neglect:** Not calling `ThreadLocal.remove()`, causing memory to leak as threads return to the pool.
32. **The @Transactional Interceptor Trap:** Calling a `@Transactional` method from within the same class (Proxy bypass).
33. **Default Connection Pool Sizes:** Leaving HikariCP at 10 connections while having 200 concurrent users.
34. **Eager Fetching by Default:** JPA `FetchType.EAGER` loading the entire database for one "Product" query.
35. **The Open Session In View (OSIV) Pattern:** Keeping DB connections open until the JSON is rendered, causing connection exhaustion.
36. **Huge Heaps, Long Pauses:** Setting a 32GB Heap without tuning GC, leading to 10-second "Stop the World" pauses.
37. **Hardcoded Profiles:** Checking `if (profile == "prod")` in code instead of using `@Profile` or Configuration classes.
38. **Unoptimized Docker Images:** Using 1GB OpenJDK images instead of slim/distroless images.
39. **Reflection Overload:** Excessive use of reflection or dynamic proxies slowing down high-traffic paths.
40. **Jackson Blockage:** Not using streaming JSON parsers for massive 100MB+ imports.
## 🐍 V. ERPNext / Python / Frappe
41. **Sync Loop in Triggers:** A `before_save` trigger that calls another save, causing a stack overflow.
42. **Frappe Background Job Overload:** Sending 50,000 tasks to Celery at once without rate limiting.
43. **Direct SQL vs ORM:** Writing `frappe.db.sql` everywhere and bypassing the permission system.
44. **Global Variable Bloat:** Using Python globals that persist across different web requests in a worker.
45. **Missing Database Commits:** Running a background job that "succeeds" but never saves data because `db.commit()` wasn't called.
46. **Bench Update Overwrites:** Modifying core files in `apps/frappe` instead of using `hooks.py`.
47. **Worker Memory Leaks:** Background workers that grow to 2GB because they are processing massive PDF reports.
48. **Insecure Server Scripts:** Allowing "Server Scripts" that can execute arbitrary Python `eval()`.
49. **Long Requests:** Letting a web request run for 60 seconds (blocking a Gunicorn worker) instead of moving it to a task.
50. **Missing Indexing on Custom Fields:** Adding a "Source ID" to Item and searching it without a DB index.
## 💎 VI. DragonflyDB / Caching
51. **Cache Stampede:** 10,000 users hitting the DB at once because a "Top Products" cache expired.
52. **The "Forever" Cache:** Forgetting to set a TTL on a session, eventually OOMing the DB.
53. **Large Key Latency:** Storing a 10MB JSON in a single Dragonfly key, slowing down the network I/O.
54. **Cache Inconsistency:** Updating the Product Price in ERPNext but not invalidating the Dragonfly cache.
55. **Cold Start Issues:** Restarting Dragonfly and crashing the Database because the cache is empty.
56. **Not Using HASH/SET:** Storing everything as flat Strings instead of using native Dragonfly types.
57. **Keyspace Notifications Overload:** Subscribing to too many event notifications, hogging CPU.
58. **Hot Keys:** One single product (like a "Black Friday Deal") taking 90% of the cache traffic.
59. **Serialization Overhead:** Using heavy Java Serialization instead of JSON or Protobuf for cached objects.
60. **Monitoring Absence:** Not knowing your "Cache Hit Ratio." (If it's <50%, the cache is useless).
## 📊 VII. ParadeDB / Search & Analytics
61. **Full-Table Search:** Running a `SELECT * FROM products WHERE name LIKE '%x%'` instead of using ParadeDB indexes.
62. **Stale Search Index:** The Search Index is 1 hour behind the actual Inventory count.
63. **Index Bloat:** Indexing the "Raw Description" HTML, making the search index 10x larger than the data.
64. **Complex Aggregations on Main DB:** Doing "Sales by Region" calculations on the transactional DB instead of a ParadeDB replica.
65. **Missing Fuzzy Search Tuning:** A search for "iPhon" returning 0 results.
66. **Re-indexing Storm:** Triggering a full search re-index every time a single product is updated.
67. **Ignoring Postgres `work_mem`:** Complex search queries spilling to disk because the RAM limit is too low.
68. **Deep Paging:** Requesting `page=1000` which forces Postgres to sort millions of rows.
69. **Lack of Rate Limiting on Search:** Bots crawling your search pages and crashing the database.
70. **No "Explain" Analysis:** Running slow search queries without ever checking the `EXPLAIN` plan.
## 🔐 VIII. Security
71. **IDOR (Insecure Direct Object Reference):** Being able to see Order #555 by changing the URL from #554.
72. **PII in Logs:** Logging the user's Credit Card number or Home Address in Plain Text.
73. **Hardcoded Secrets:** Storing the Stripe API Key in `application.yml` and pushing it to GitHub.
74. **Shadow APIs:** Old versions of an API (`/v1/order`) left running without security patches.
75. **Missing Rate Limiting:** Allowing 1,000 "Coupon Apply" attempts per second from one IP.
76. **Insecure CORS:** Allowing `Access-Control-Allow-Origin: *`.
77. **Weak JWT Secrets:** Using "secret123" to sign your login tokens.
78. **Insecure File Uploads:** Allowing users to upload `.php` or `.sh` files as "Product Images."
79. **Lack of Audit Logs:** Not knowing *who* changed a Product Price to $0.01.
80. **Unencrypted Internal Traffic:** Sending data between Spring Boot and ERPNext in HTTP instead of HTTPS.
## 🔋 IX. Resource Management
81. **Socket Exhaustion:** Not closing HTTP clients, leaving thousands of "TIME_WAIT" connections.
82. **File Descriptor Leaks:** Opening CSVs for import and never calling `.close()`.
83. **Memory Fragmentation:** Constant allocation of small objects in a tight loop.
84. **Zombie Processes:** Background workers that died but are still holding onto RAM.
85. **DNS Timeout Block:** A slow DNS server causing all outbound API calls to hang for 30 seconds.
86. **Disk Space Death:** Log files filling up the entire server because rotation was never set up.
87. **Over-Threading:** Starting 500 threads on a 2-core CPU, causing constant context-switching.
88. **Static Collection Leak:** A `static List<String> history` that never gets cleared.
89. **Large Request Buffering:** Reading a 50MB file into a `byte[]` instead of using an `InputStream`.
90. **Inconsistent Timezones:** Half the system in UTC, half in local time, leading to "Double Expiry" bugs.
## 🛒 X. E-Commerce Domain Logic
91. **The Oversell Race Condition:** Two people buying the last item because the inventory wasn't "Locked" or "Incremented" atomically.
92. **Rounding Errors:** Using `float` for currency and losing $0.01 on every 100 transactions.
93. **Ignoring Abandoned Carts:** Keeping inventory "Reserved" forever for a user who closed their browser.
94. **Hardcoded Shipping Rules:** Writing `if (country == 'US')` in Java instead of a database-driven policy.
95. **Price Versioning Absence:** Changing a product price and accidentally changing the price of orders placed 1 month ago.
96. **Missing "Out of Stock" UI:** Letting a user reach the "Pay" button before telling them the item is gone.
97. **Partial Shipment Chaos:** Moving an order to "Shipped" when only 1 of 5 items was sent.
98. **Double Counting Discounts:** Applying a 10% coupon to an already discounted "Sale" item incorrectly.
99. **Missing Tax Nexus:** Failing to calculate tax correctly for different regions/states.
100. **The "Finish Line" Fallacy:** Thinking the architecture is "done" once it works on your local machine.
---
### 🛡️ Final Strategy:
Don't try to fix all 100 at once. Pick the **Top 5** that affect your **Availability** and **Data Integrity** first.
- Availability = #1, #31, #51, #81.
- Integrity = #25, #91, #92, #95.
