-- Optimizing Order lookups by email
CREATE INDEX idx_orders_email ON orders(email);

-- Optimizing Order Status lookups (used by ERP sync)
CREATE INDEX idx_orders_status ON orders(order_status);

-- Optimizing Product lookups by item_code
CREATE INDEX idx_products_item_code ON products(item_code);

-- Optimizing Cart lookups by cart_id (if not primary)
-- CREATE INDEX idx_carts_cart_id ON carts(cart_id);

-- Optimizing search by special price
CREATE INDEX idx_products_special_price ON products(special_price);

-- Optimizing Review lookups by product_id
CREATE INDEX idx_reviews_product_id ON reviews(product_id);
