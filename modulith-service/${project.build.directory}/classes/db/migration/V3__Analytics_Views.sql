-- Metabase/Analytics Views

-- 1. Daily Revenue View
CREATE OR REPLACE VIEW view_daily_revenue AS
SELECT 
    order_date,
    COUNT(order_id) as total_orders,
    SUM(total_amount) as gross_revenue,
    AVG(total_amount) as average_order_value
FROM orders
WHERE order_status NOT IN ('CANCELLED', 'RETURNED')
GROUP BY order_date;

-- 2. Top Products by Volume
CREATE OR REPLACE VIEW view_top_products AS
SELECT 
    p.product_id,
    p.product_name,
    p.item_code,
    c.category_name,
    SUM(oi.quantity) as total_units_sold,
    SUM(oi.quantity * oi.ordered_price) as total_revenue
FROM products p
JOIN categories c ON p.category_id = c.category_id
JOIN order_items oi ON p.product_id = oi.product_id
JOIN orders o ON oi.order_id = o.order_id
WHERE o.order_status NOT IN ('CANCELLED', 'RETURNED')
GROUP BY p.product_id, p.product_name, p.item_code, c.category_name;

-- 3. Inventory Health
CREATE OR REPLACE VIEW view_inventory_health AS
SELECT 
    p.product_id,
    p.product_name,
    p.quantity as current_stock,
    CASE 
        WHEN p.quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN p.quantity < 10 THEN 'LOW_STOCK'
        ELSE 'OPTIMAL'
    END as status
FROM products p;

-- 4. User Engagement (simplified)
CREATE OR REPLACE VIEW view_user_activity AS
SELECT 
    u.email,
    u.first_name,
    u.last_name,
    (SELECT COUNT(*) FROM orders o WHERE o.email = u.email) as total_purchases,
    (SELECT SUM(total_amount) FROM orders o WHERE o.email = u.email) as customer_lifetime_value
FROM users u;
