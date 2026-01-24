-- Enable UUID extension if needed (not strictly used yet but good practice)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Tenants
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    erp_next_url VARCHAR(255),
    erp_next_api_key VARCHAR(255),
    erp_next_api_secret VARCHAR(255),
    environment VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

-- 2. Roles
CREATE TABLE role (
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(255)
);

-- 3. Addresses
CREATE TABLE addresses (
    address_id BIGSERIAL PRIMARY KEY,
    street VARCHAR(255),
    building_name VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    pincode VARCHAR(255),
    is_default_shipping BOOLEAN DEFAULT FALSE,
    is_default_billing BOOLEAN DEFAULT FALSE,
    label VARCHAR(255),
    receiver_phone_number VARCHAR(255)
);

-- 4. Users
CREATE TABLE app_users (
    user_id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    mobile_number VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    reward_points INTEGER,
    customer_group VARCHAR(255),
    verification_code VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    reset_token VARCHAR(255),
    reset_token_expiry TIMESTAMP,
    avatar_url VARCHAR(255),
    date_of_birth DATE,
    gender VARCHAR(255),
    account_status VARCHAR(255)
);

-- User Associations
CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_users(user_id),
    FOREIGN KEY (role_id) REFERENCES role(role_id)
);

CREATE TABLE user_address (
    user_id BIGINT NOT NULL,
    address_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_users(user_id),
    FOREIGN KEY (address_id) REFERENCES addresses(address_id)
);

CREATE TABLE user_preferences (
    user_id BIGINT NOT NULL,
    pref_value VARCHAR(255),
    pref_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, pref_key),
    FOREIGN KEY (user_id) REFERENCES app_users(user_id)
);

CREATE TABLE user_friends (
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES app_users(user_id),
    FOREIGN KEY (friend_id) REFERENCES app_users(user_id)
);

CREATE TABLE user_payment_methods (
    user_id BIGINT NOT NULL,
    vault_token VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES app_users(user_id)
);

-- 5. Categories
CREATE TABLE category (
    category_id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(255)
);

-- 6. Products
-- Note: Logic in Service uses GenerationType.AUTO for Product.
-- Creating a sequence to be safe, though IDENTITY is preferred.
CREATE SEQUENCE IF NOT EXISTS products_seq START 1 INCREMENT 50;

CREATE TABLE products (
    product_id BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('products_seq'),
    product_name VARCHAR(255),
    item_code VARCHAR(255),
    image VARCHAR(255),
    description TEXT,
    quantity INTEGER,
    price DOUBLE PRECISION,
    discount DOUBLE PRECISION,
    special_price DOUBLE PRECISION,
    brand VARCHAR(255),
    category_id BIGINT,
    is_customizable BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(category_id)
);

CREATE TABLE product_variants (
    variant_id BIGSERIAL PRIMARY KEY,
    item_code VARCHAR(255),
    color VARCHAR(255),
    size VARCHAR(255),
    material VARCHAR(255),
    stock_quantity INTEGER,
    product_id BIGINT,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE product_media (
    media_id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255),
    url VARCHAR(255),
    display_order INTEGER,
    blur_hash VARCHAR(255),
    product_id BIGINT,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE product_reviews (
    review_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT,
    user_id BIGINT,
    user_name VARCHAR(255),
    rating INTEGER,
    comment TEXT,
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- 7. Coupons
CREATE TABLE coupons (
    coupon_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(255),
    discount_value DOUBLE PRECISION,
    min_order_amount DOUBLE PRECISION,
    max_discount_amount DOUBLE PRECISION,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    usage_limit INTEGER,
    used_count INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
);

CREATE TABLE coupon_categories (
    coupon_id BIGINT NOT NULL,
    category_name VARCHAR(255),
    FOREIGN KEY (coupon_id) REFERENCES coupons(coupon_id)
);

-- 8. Order Dependencies
CREATE TABLE payment (
    payment_id BIGSERIAL PRIMARY KEY,
    payment_method VARCHAR(255),
    pg_payment_id VARCHAR(255),
    pg_order_id VARCHAR(255),
    pg_status VARCHAR(255),
    pg_signature VARCHAR(255)
);

CREATE TABLE shipments (
    shipment_id BIGSERIAL PRIMARY KEY,
    carrier VARCHAR(255),
    awb_number VARCHAR(255),
    status VARCHAR(255),
    track_url VARCHAR(255),
    external_order_id VARCHAR(255),
    external_shipment_id VARCHAR(255),
    courier_name VARCHAR(255)
);

-- 9. Orders
CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    order_date DATE,
    total_amount DOUBLE PRECISION,
    order_status VARCHAR(255),
    coupon_code VARCHAR(255),
    discount_amount DOUBLE PRECISION DEFAULT 0.0,
    erp_next_order_name VARCHAR(255),
    payment_id BIGINT UNIQUE,
    shipment_id BIGINT UNIQUE,
    FOREIGN KEY (payment_id) REFERENCES payment(payment_id),
    FOREIGN KEY (shipment_id) REFERENCES shipments(shipment_id)
);

CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT,
    product_id BIGINT,
    variant_id BIGINT,
    quantity INTEGER,
    ordered_price DOUBLE PRECISION,
    discount DOUBLE PRECISION,
    product_name VARCHAR(255),
    item_code VARCHAR(255),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id)
);

-- 10. Cart
CREATE TABLE cart (
    cart_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    total_price DOUBLE PRECISION,
    address_id BIGINT,
    coupon_code VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES app_users(user_id)
);

CREATE TABLE cart_items (
    cart_item_id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT,
    product_id BIGINT,
    item_code VARCHAR(255),
    product_name VARCHAR(255),
    quantity INTEGER,
    discount DOUBLE PRECISION,
    product_price DOUBLE PRECISION,
    FOREIGN KEY (cart_id) REFERENCES cart(cart_id)
);

