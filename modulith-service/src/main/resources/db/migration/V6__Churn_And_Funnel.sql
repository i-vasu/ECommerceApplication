-- Churn Risk View (Users with no orders in last 60 days but had orders before)
CREATE OR REPLACE VIEW view_churn_risk AS
SELECT 
    u.user_id,
    u.email,
    u.first_name,
    MAX(o.order_date) as last_order_date,
    COUNT(o.order_id) as total_orders
FROM users u
JOIN orders o ON u.user_id = o.user_id
GROUP BY u.user_id, u.email, u.first_name
HAVING MAX(o.order_date) < CURRENT_TIMESTAMP - INTERVAL '60 days'
ORDER BY last_order_date DESC;

-- Cart Conversion funnel View
CREATE OR REPLACE VIEW view_funnel_stats AS
SELECT 
    (SELECT COUNT(*) FROM activity_logs WHERE activity_type = 'SEARCH') as total_searches,
    (SELECT COUNT(*) FROM activity_logs WHERE activity_type = 'CART_ADD') as total_add_to_carts,
    (SELECT COUNT(*) FROM orders) as total_orders;
