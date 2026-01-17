## 2026-01-17 - Eager Fetching on High-Traffic Entities
**Learning:** Found `FetchType.EAGER` on `Product.cartItems` (mapped as `products`). This caused every product fetch (including lists) to join with the `cart_items` table, and subsequently `carts` and `users` tables.
**Action:** Default to `LAZY` for `@OneToMany` relationships. Only use `EAGER` when the related data is absolutely required in every use case of the parent entity.
