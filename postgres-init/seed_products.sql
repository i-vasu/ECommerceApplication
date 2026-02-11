-- Seed Data for Luxury Products
-- Generated from add_products_to_erpnext.py

-- Ensure relevant tables exist (this runs after flyway migrations usually, but for docker-entrypoint-initdb.d it runs first)
-- We assume the schema is created by the application or Flyway. 
-- However, for a pure seed script that runs on init, we might need to insert into the specific table used by modulith-catalog.

-- Clean up existing data to avoid duplicates if re-running
-- Insert Products with HSN Codes
DELETE FROM products WHERE sku LIKE 'VA-%';

INSERT INTO products (sku, name, description, price, currency, brand, category, image_url, active, created_at, updated_at, hsn_code) VALUES
('VA-SRW-006', 'Velvet Royal Sherwani', 'Exquisite midnight blue velvet sherwani with silver Zardosi embroidery. The pinnacle of heritage luxury.', 25000.00, 'INR', 'Royal Heritage', 'Ethnic Wear', 'https://images.unsplash.com/photo-1594938298603-c8148c4dae35?q=80&w=800', true, NOW(), NOW(), '6203'),
('VA-DEN-007', 'Indigo Denim Jacket', 'Premium raw indigo denim jacket with copper hardware. Crafted for the modern nomad.', 4500.00, 'INR', 'Modern Living', 'Casual Wear', 'https://images.unsplash.com/photo-1576905307817-a1611eb245a4?q=80&w=800', true, NOW(), NOW(), '6202'),
('VA-ACC-008', 'Gold Embroidered Clutch', 'Hand-crafted silk clutch with intricate gold threadwork and semi-precious stone embellishments.', 12000.00, 'INR', 'Craftsmanship', 'Accessories', 'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?q=80&w=800', true, NOW(), NOW(), '4202'),
('VA-DRS-009', 'Ivory Lace Cocktail Dress', 'Delicate ivory chantilly lace dress with a silk satin lining. Timeless elegance reimagined.', 18500.00, 'INR', 'Royal Heritage', 'Luxury Gowns', 'https://images.unsplash.com/photo-1518917232260-0e613c77553f?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-KRT-010', 'Chanderi Silk Kurta Set', 'Handwoven Chanderi silk kurta with palazzo pants. Adorned with traditional block prints from Rajasthan.', 8900.00, 'INR', 'Craftsmanship', 'Ethnic Wear', 'https://images.unsplash.com/photo-1583391733956-6c78276477e2?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-BLZ-011', 'Charcoal Wool Blazer', 'Italian merino wool blazer with peak lapels. Tailored for the discerning professional.', 15500.00, 'INR', 'Modern Living', 'Casual Wear', 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?q=80&w=800', true, NOW(), NOW(), '6203'),
('VA-SHL-012', 'Pashmina Embroidered Shawl', '100% Kashmiri pashmina shawl with hand-embroidered paisley motifs. A timeless heirloom.', 22000.00, 'INR', 'Craftsmanship', 'Accessories', 'https://images.unsplash.com/photo-1601924994987-69e26d50dc26?q=80&w=800', true, NOW(), NOW(), '6214'),
('VA-GWN-013', 'Emerald Velvet Ball Gown', 'Regal emerald velvet ball gown with crystal embellishments. Designed for grand occasions.', 45000.00, 'INR', 'Royal Heritage', 'Luxury Gowns', 'https://images.unsplash.com/photo-1566174053879-31528523f8ae?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-LHG-014', 'Banarasi Silk Lehenga', 'Opulent Banarasi silk lehenga with 24k gold zari work. A bridal masterpiece.', 65000.00, 'INR', 'Royal Heritage', 'Ethnic Wear', 'https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-TRS-015', 'Linen Trousers', 'Breathable Belgian linen trousers in classic beige. Perfect for summer elegance.', 3200.00, 'INR', 'Modern Living', 'Casual Wear', 'https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-JTI-016', 'Kundan Polki Juttis', 'Handcrafted leather juttis embellished with Kundan and Polki stones. Traditional artistry meets comfort.', 8500.00, 'INR', 'Craftsmanship', 'Accessories', 'https://images.unsplash.com/photo-1543163521-1bf539c55dd2?q=80&w=800', true, NOW(), NOW(), '6403'),
('VA-CPE-017', 'Burgundy Cape Gown', 'Dramatic burgundy gown with flowing cape sleeves. Statement-making sophistication.', 32000.00, 'INR', 'Royal Heritage', 'Luxury Gowns', 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-BND-018', 'Bandhani Silk Dupatta', 'Traditional Bandhani tie-dye silk dupatta from Gujarat. Vibrant heritage in every knot.', 4800.00, 'INR', 'Craftsmanship', 'Ethnic Wear', 'https://images.unsplash.com/photo-1610652492500-ded49ceeb378?q=80&w=800', true, NOW(), NOW(), '6214'),
('VA-SWT-019', 'Cashmere Turtleneck', 'Pure Mongolian cashmere turtleneck in ivory. Luxurious warmth and minimalist elegance.', 12800.00, 'INR', 'Modern Living', 'Casual Wear', 'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?q=80&w=800', true, NOW(), NOW(), '6102'),
('VA-BLT-020', 'Leather Statement Belt', 'Hand-tooled vegetable-tanned leather belt with brass buckle. Artisan craftsmanship.', 6500.00, 'INR', 'Craftsmanship', 'Accessories', 'https://images.unsplash.com/photo-1624222247344-550fb60583c2?q=80&w=800', true, NOW(), NOW(), '4203'),
('VA-SEQ-021', 'Sequin Mermaid Gown', 'Shimmering champagne sequin mermaid gown. Red carpet ready glamour.', 38000.00, 'INR', 'Royal Heritage', 'Luxury Gowns', 'https://images.unsplash.com/photo-1566174053879-31528523f8ae?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-KFT-022', 'Ikat Print Kaftan', 'Flowing silk kaftan with traditional Ikat prints. Bohemian luxury for resort wear.', 5600.00, 'INR', 'Craftsmanship', 'Ethnic Wear', 'https://images.unsplash.com/photo-1583391733981-e8c9f5e8c6a9?q=80&w=800', true, NOW(), NOW(), '6204'),
('VA-OVR-023', 'Wool Overcoat', 'Double-breasted camel wool overcoat. Timeless winter sophistication.', 28000.00, 'INR', 'Modern Living', 'Casual Wear', 'https://images.unsplash.com/photo-1539533018447-63fcce2678e3?q=80&w=800', true, NOW(), NOW(), '6201'),
('VA-BRC-024', 'Pearl Choker Necklace', 'South Sea pearl choker with diamond clasp. Heirloom-quality elegance.', 45000.00, 'INR', 'Royal Heritage', 'Accessories', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?q=80&w=800', true, NOW(), NOW(), '7116'),
('VA-TUL-025', 'Blush Tulle Gown', 'Romantic blush pink tulle gown with floral appliqués. Ethereal beauty personified.', 42000.00, 'INR', 'Royal Heritage', 'Luxury Gowns', 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?q=80&w=800', true, NOW(), NOW(), '6204');

-- Seed Inventory (Default 50 units per item)
DELETE FROM inventory WHERE item_code LIKE 'VA-%';

INSERT INTO inventory (item_code, quantity, reserved_quantity, warehouse_id, version) VALUES
('VA-SRW-006', 50, 0, 'MAIN', 0),
('VA-DEN-007', 50, 0, 'MAIN', 0),
('VA-ACC-008', 50, 0, 'MAIN', 0),
('VA-DRS-009', 50, 0, 'MAIN', 0),
('VA-KRT-010', 50, 0, 'MAIN', 0),
('VA-BLZ-011', 50, 0, 'MAIN', 0),
('VA-SHL-012', 50, 0, 'MAIN', 0),
('VA-GWN-013', 50, 0, 'MAIN', 0),
('VA-LHG-014', 50, 0, 'MAIN', 0),
('VA-TRS-015', 50, 0, 'MAIN', 0),
('VA-JTI-016', 50, 0, 'MAIN', 0),
('VA-CPE-017', 50, 0, 'MAIN', 0),
('VA-BND-018', 50, 0, 'MAIN', 0),
('VA-SWT-019', 50, 0, 'MAIN', 0),
('VA-BLT-020', 50, 0, 'MAIN', 0),
('VA-SEQ-021', 50, 0, 'MAIN', 0),
('VA-KFT-022', 50, 0, 'MAIN', 0),
('VA-OVR-023', 50, 0, 'MAIN', 0),
('VA-BRC-024', 50, 0, 'MAIN', 0),
('VA-TUL-025', 50, 0, 'MAIN', 0);
