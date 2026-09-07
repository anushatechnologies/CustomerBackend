-- ========================================================
-- Flyway Database Migration: V1__init_schema.sql
-- HinchMart B2B E-Commerce & Customer Backend Schema Baseline
-- Clean Customer & Seller Architecture (No Vendors, No UserProfiles)
-- ========================================================

-- --------------------------------------------------------
-- 1. Customers Table (Central Customer/Buyer Entity)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(255) NOT NULL DEFAULT 'BUYER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 2. Sellers Table (Merchant Entity)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS sellers (
    seller_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255) NOT NULL UNIQUE,
    pan_number VARCHAR(255) UNIQUE,
    aadhaar_number VARCHAR(255) UNIQUE,
    pan_card_url VARCHAR(2000),
    aadhaar_card_url VARCHAR(2000),
    company_name VARCHAR(255),
    business_type VARCHAR(255),
    gstin VARCHAR(255) UNIQUE,
    gst_certificate_url VARCHAR(2000),
    business_address VARCHAR(500),
    state VARCHAR(255),
    city VARCHAR(255),
    pincode VARCHAR(255),
    bank_name VARCHAR(255),
    account_holder_name VARCHAR(255),
    account_number VARCHAR(255) UNIQUE,
    ifsc_code VARCHAR(255),
    account_type VARCHAR(255),
    onboarding_status VARCHAR(50) NOT NULL DEFAULT 'STEP_1',
    verification_status VARCHAR(50) DEFAULT 'PENDING',
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 3. Categories Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    product_count INT DEFAULT 0,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 4. Banners Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS banners (
    banner_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    image_url TEXT,
    link_type VARCHAR(255),
    link_value VARCHAR(255),
    position VARCHAR(255),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    start_date DATETIME,
    end_date DATETIME,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 5. Blog Articles Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS blog_articles (
    article_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    excerpt TEXT,
    content LONGTEXT NOT NULL,
    author VARCHAR(255),
    category VARCHAR(255),
    tags VARCHAR(255),
    read_time_minutes INT DEFAULT 5,
    image_url TEXT,
    is_published BOOLEAN DEFAULT TRUE,
    published_at DATETIME,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 6. News Items Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS news_items (
    news_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content LONGTEXT,
    category VARCHAR(255) DEFAULT 'COMMODITY_PRICES',
    source VARCHAR(255),
    source_url VARCHAR(255),
    image_url TEXT,
    price_change_percentage DOUBLE,
    trend_direction VARCHAR(255),
    published_at DATETIME,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 7. Rental Equipment Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rental_equipment (
    equipment_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    model VARCHAR(255),
    specifications TEXT,
    daily_rate DECIMAL(12, 2) NOT NULL,
    weekly_rate DECIMAL(12, 2),
    monthly_rate DECIMAL(12, 2),
    deposit_amount DECIMAL(12, 2) DEFAULT 0.00,
    image_url TEXT,
    location VARCHAR(255),
    operator_available BOOLEAN DEFAULT TRUE,
    operator_daily_charge DECIMAL(10, 2) DEFAULT 1200.00,
    is_available BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 8. Chat Conversations Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_conversations (
    conversation_id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    seller_id INT NOT NULL,
    topic VARCHAR(255) DEFAULT 'GENERAL',
    reference_id VARCHAR(255),
    title VARCHAR(255),
    last_message_text TEXT,
    last_message_timestamp DATETIME,
    unread_buyer INT DEFAULT 0,
    unread_seller INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 9. Subcategories Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS subcategories (
    subcategory_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    product_count INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_subcategories_category FOREIGN KEY (category_id) REFERENCES categories (category_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 10. Customer Documents Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_documents (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    document_number VARCHAR(100),
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_size VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(255),
    expires_on DATE,
    uploaded_at DATETIME NOT NULL,
    verified_at DATETIME,
    CONSTRAINT fk_customer_docs_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 11. User Addresses Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_addresses (
    address_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    site_name VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    pincode VARCHAR(255) NOT NULL,
    landmark VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    has_heavy_vehicle_access BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_user_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 12. Seller Documents Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS seller_documents (
    document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(150),
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(2000) NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    file_size BIGINT,
    remarks VARCHAR(1000),
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_seller_document_type UNIQUE (seller_id, document_type),
    CONSTRAINT fk_seller_docs_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 13. Seller Warehouses Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS seller_warehouses (
    warehouse_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    contact_person VARCHAR(255),
    phone VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    pincode VARCHAR(255),
    address VARCHAR(500),
    capacity_tons INT,
    status VARCHAR(255) DEFAULT 'Active',
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_seller_warehouses_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 14. Seller Discounts Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS seller_discounts (
    discount_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(12, 2) NOT NULL,
    minimum_order_amount DECIMAL(12, 2) DEFAULT 0.00,
    max_discount_amount DECIMAL(12, 2),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    active BOOLEAN DEFAULT TRUE,
    rejection_reason VARCHAR(255),
    admin_note VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_seller_discounts_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 15. Wallets Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL UNIQUE,
    balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(255) NOT NULL DEFAULT 'INR',
    loyalty_points INT DEFAULT 0,
    tier VARCHAR(255) DEFAULT 'GOLD',
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_wallets_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 16. Carts Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS carts (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    applied_coupon VARCHAR(255),
    delivery_charge DECIMAL(10, 2) DEFAULT 4500.00,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 17. RFQs Table (Request for Quotation)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rfqs (
    rfq_id INT AUTO_INCREMENT PRIMARY KEY,
    rfq_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id INT,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    product_material VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(255) NOT NULL,
    technical_grade VARCHAR(255),
    mtc_required BOOLEAN DEFAULT TRUE,
    delivery_location VARCHAR(255) NOT NULL,
    required_by_date DATE,
    site_access VARCHAR(255),
    crane_required BOOLEAN DEFAULT FALSE,
    target_budget DECIMAL(14, 2),
    payment_terms VARCHAR(255),
    specifications TEXT,
    boq_attachment_url VARCHAR(255),
    status VARCHAR(255) DEFAULT 'OPEN',
    quotes_count INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_rfqs_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 18. RFQ Quotations Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rfq_quotations (
    quote_id INT AUTO_INCREMENT PRIMARY KEY,
    rfq_id INT,
    quotation_number VARCHAR(255),
    buyer_name VARCHAR(255),
    buyer_email VARCHAR(255),
    freight_charges DECIMAL(12, 2),
    delivery_timeline VARCHAR(255),
    items_json TEXT,
    seller_id INT NOT NULL,
    seller_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL,
    delivery_lead_time_days INT,
    payment_terms_offered VARCHAR(255),
    mtc_included BOOLEAN DEFAULT TRUE,
    freight_included BOOLEAN DEFAULT TRUE,
    valid_until DATETIME,
    seller_rating DOUBLE DEFAULT 4.8,
    status VARCHAR(255) DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_rfq_quotations_rfq FOREIGN KEY (rfq_id) REFERENCES rfqs (rfq_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 19. RFQ Questions Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rfq_questions (
    question_id INT AUTO_INCREMENT PRIMARY KEY,
    rfq_id INT NOT NULL,
    question TEXT NOT NULL,
    response TEXT,
    status VARCHAR(255) DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    answered_at DATETIME,
    CONSTRAINT fk_rfq_questions_rfq FOREIGN KEY (rfq_id) REFERENCES rfqs (rfq_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 20. Orders Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id INT,
    address_id INT,
    delivery_location VARCHAR(255),
    subtotal DECIMAL(14, 2),
    discount DECIMAL(14, 2) DEFAULT 0.00,
    coupon_code VARCHAR(255),
    taxable_amount DECIMAL(14, 2),
    cgst DECIMAL(14, 2) DEFAULT 0.00,
    sgst DECIMAL(14, 2) DEFAULT 0.00,
    igst DECIMAL(14, 2) DEFAULT 0.00,
    total_gst DECIMAL(14, 2) DEFAULT 0.00,
    freight_charge DECIMAL(14, 2) DEFAULT 0.00,
    crane_unloading_charge DECIMAL(14, 2) DEFAULT 0.00,
    total_amount DECIMAL(14, 2) NOT NULL,
    payment_method VARCHAR(255) DEFAULT 'RAZORPAY',
    payment_status VARCHAR(255) DEFAULT 'PENDING',
    order_status VARCHAR(255) DEFAULT 'PLACED',
    po_number VARCHAR(255),
    delivery_slot VARCHAR(255),
    delivery_instructions TEXT,
    requires_crane_unloading BOOLEAN DEFAULT FALSE,
    carrier_name VARCHAR(255),
    vehicle_number VARCHAR(255),
    driver_name VARCHAR(255),
    tracking_number VARCHAR(255),
    estimated_delivery DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 21. Purchase Orders Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchase_orders (
    po_id INT AUTO_INCREMENT PRIMARY KEY,
    po_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    seller_id INT NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL',
    delivery_date DATE,
    billing_address TEXT,
    shipping_address TEXT,
    payment_terms VARCHAR(255) DEFAULT 'NET_30',
    notes TEXT,
    rejection_reason VARCHAR(255),
    approved_at DATETIME,
    approved_by VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_purchase_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 22. Support Tickets Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS support_tickets (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(255) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL',
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'OPEN',
    order_id INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_support_tickets_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 23. Rental Bookings Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rental_bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    equipment_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    site_address TEXT NOT NULL,
    operator_required BOOLEAN DEFAULT FALSE,
    total_days INT NOT NULL,
    rate_per_day DECIMAL(12, 2) NOT NULL,
    operator_cost DECIMAL(12, 2) DEFAULT 0.00,
    deposit_amount DECIMAL(12, 2) DEFAULT 0.00,
    total_cost DECIMAL(14, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_rental_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_bookings_equipment FOREIGN KEY (equipment_id) REFERENCES rental_equipment (equipment_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 24. Reward Vouchers Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS reward_vouchers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    code VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(10, 2) NOT NULL,
    min_order_value DECIMAL(12, 2) DEFAULT 0.00,
    max_discount DECIMAL(10, 2),
    expiry_date DATETIME,
    is_redeemed BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_reward_vouchers_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 25. Chat Messages Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    attachment_url TEXT,
    message_type VARCHAR(50) DEFAULT 'TEXT',
    is_read BOOLEAN DEFAULT FALSE,
    timestamp DATETIME NOT NULL,
    CONSTRAINT fk_chat_messages_conv FOREIGN KEY (conversation_id) REFERENCES chat_conversations (conversation_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 26. Brands Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS brands (
    brand_id INT AUTO_INCREMENT PRIMARY KEY,
    subcategory_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    product_count INT DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_brands_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategories (subcategory_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 27. Wallet Transactions Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wallet_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    source VARCHAR(100),
    reference_id VARCHAR(255),
    description VARCHAR(255),
    balance_after DECIMAL(14, 2),
    timestamp DATETIME NOT NULL,
    CONSTRAINT fk_wallet_txns_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (wallet_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 28. Order Items Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    image_url VARCHAR(255),
    quantity INT NOT NULL,
    unit VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    original_price DECIMAL(12, 2),
    applied_tier VARCHAR(255),
    gst_rate DECIMAL(5, 2),
    line_total DECIMAL(14, 2) NOT NULL,
    line_gst DECIMAL(14, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 29. Order Tracking Checkpoints Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_tracking_checkpoints (
    checkpoint_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    status VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    description TEXT,
    timestamp DATETIME NOT NULL,
    CONSTRAINT fk_tracking_checkpoints_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 30. Purchase Order Items Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchase_order_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    po_id INT NOT NULL,
    product_id INT,
    product_title VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    tax_rate DECIMAL(5, 2) DEFAULT 18.00,
    line_total DECIMAL(14, 2) NOT NULL,
    CONSTRAINT fk_po_items_po FOREIGN KEY (po_id) REFERENCES purchase_orders (po_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 31. Ticket Messages Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    sender_id INT NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    sender_name VARCHAR(255),
    content TEXT NOT NULL,
    attachment_url TEXT,
    timestamp DATETIME NOT NULL,
    CONSTRAINT fk_ticket_messages_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (ticket_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 32. Products Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    brand_id INT,
    seller_id INT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    sku VARCHAR(255),
    description TEXT,
    image_url TEXT,
    images TEXT,
    price DECIMAL(12, 2) NOT NULL,
    selling_price DECIMAL(12, 2),
    mrp DECIMAL(12, 2),
    unit VARCHAR(255) NOT NULL,
    moq INT DEFAULT 1,
    stock_qty INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    is_24_hour_delivery BOOLEAN DEFAULT FALSE,
    rating DOUBLE DEFAULT 0.0,
    review_count INT DEFAULT 0,
    gst_rate DECIMAL(5, 2) DEFAULT 18.00,
    hsn_code VARCHAR(255),
    specifications TEXT,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(255),
    bulk_pricing_tiers TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (brand_id) ON DELETE SET NULL,
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 33. Seller Inventory Adjustments Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS seller_inventory_adjustments (
    adjustment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    product_id INT NOT NULL,
    warehouse_id INT,
    adjustment_type VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    previous_stock INT,
    new_stock INT,
    reason VARCHAR(500),
    created_at DATETIME,
    CONSTRAINT fk_inv_adj_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_adj_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_adj_warehouse FOREIGN KEY (warehouse_id) REFERENCES seller_warehouses (warehouse_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 34. Cart Items Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    cart_item_id INT AUTO_INCREMENT PRIMARY KEY,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (cart_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 35. Wishlist Items Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS wishlist_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    product_id INT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_wishlist_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 36. Product Reviews Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    customer_id INT NOT NULL,
    order_id INT NOT NULL,
    order_item_id INT NOT NULL UNIQUE,
    rating INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    comment TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    helpful_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (order_item_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 37. Review Images Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS review_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_review_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_review_images_review FOREIGN KEY (product_review_id) REFERENCES product_reviews (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 38. Review Helpful Votes Table
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS review_helpful_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    customer_id INT NOT NULL,
    CONSTRAINT uk_review_helpful_vote UNIQUE (review_id, customer_id),
    CONSTRAINT fk_review_votes_review FOREIGN KEY (review_id) REFERENCES product_reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_votes_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
