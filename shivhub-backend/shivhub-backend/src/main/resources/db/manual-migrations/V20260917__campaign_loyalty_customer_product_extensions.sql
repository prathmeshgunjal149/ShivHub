-- ShivHub additive migration. Run once on MySQL 8 before enabling the release.
-- It never drops a table or column. Back up production before every schema change.

ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL;
ALTER TABLE customer_profiles ADD COLUMN IF NOT EXISTS city VARCHAR(100) NULL;
ALTER TABLE customer_profiles ADD COLUMN IF NOT EXISTS district VARCHAR(100) NULL;
ALTER TABLE customer_profiles ADD COLUMN IF NOT EXISTS state VARCHAR(100) NULL;
ALTER TABLE customer_profiles ADD COLUMN IF NOT EXISTS pincode VARCHAR(12) NULL;
ALTER TABLE customer_profiles ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_specifications TEXT NULL;
ALTER TABLE offline_bills ADD COLUMN IF NOT EXISTS loyalty_points_redeemed INT NOT NULL DEFAULT 0;
ALTER TABLE offline_bills ADD COLUMN IF NOT EXISTS loyalty_discount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE marketing_campaigns ADD COLUMN IF NOT EXISTS seller_id BIGINT NULL;
ALTER TABLE marketing_campaigns ADD COLUMN IF NOT EXISTS offer_details TEXT NULL;
ALTER TABLE marketing_deliveries ADD COLUMN IF NOT EXISTS customer_profile_id BIGINT NULL;
ALTER TABLE marketing_deliveries ADD COLUMN IF NOT EXISTS failure_reason TEXT NULL;

CREATE TABLE IF NOT EXISTS customer_loyalty_wallet (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_profile_id BIGINT NOT NULL,
  available_points BIGINT NOT NULL DEFAULT 0,
  lifetime_earned_points BIGINT NOT NULL DEFAULT 0,
  lifetime_redeemed_points BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_loyalty_wallet_customer (customer_profile_id),
  CONSTRAINT fk_loyalty_wallet_customer FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id)
);

CREATE TABLE IF NOT EXISTS loyalty_point_transactions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_profile_id BIGINT NOT NULL,
  transaction_type VARCHAR(16) NOT NULL,
  source_type VARCHAR(24) NOT NULL,
  source_id BIGINT NOT NULL,
  seller_id BIGINT NULL,
  shop_id BIGINT NULL,
  points BIGINT NOT NULL,
  balance_after BIGINT NOT NULL,
  remarks TEXT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_loyalty_source_type_transaction (source_type, source_id, transaction_type),
  KEY idx_loyalty_transaction_customer (customer_profile_id, created_at),
  KEY idx_loyalty_transaction_seller (seller_id, created_at),
  CONSTRAINT fk_loyalty_transaction_customer FOREIGN KEY (customer_profile_id) REFERENCES customer_profiles(id)
);

CREATE TABLE IF NOT EXISTS loyalty_settings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  scope_type VARCHAR(12) NOT NULL,
  seller_id BIGINT NULL,
  minimum_purchase_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  points_per_purchase_unit BIGINT NOT NULL DEFAULT 1,
  purchase_unit_in_rupees DECIMAL(12,2) NOT NULL DEFAULT 100.00,
  point_value_in_rupees DECIMAL(12,2) NOT NULL DEFAULT 1.00,
  maximum_points_per_sale BIGINT NOT NULL DEFAULT 0,
  is_active BIT NOT NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uk_loyalty_scope_seller (scope_type, seller_id)
);

CREATE TABLE IF NOT EXISTS birthday_greeting_settings (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email_subject VARCHAR(250) NOT NULL,
  message_content TEXT NOT NULL,
  coupon_code VARCHAR(100) NULL,
  banner_url VARCHAR(2000) NULL,
  is_active BIT NOT NULL DEFAULT b'0',
  PRIMARY KEY (id)
);

-- One-time online-customer profile backfill. It is idempotent and does not create
-- profiles for anonymous legacy walk-in bills, which is what previously inflated counts.
INSERT INTO customer_profiles (online_user_id, name, mobile, email, communication_consent, date_of_birth, created_at, updated_at)
SELECT u.id, u.name, u.mobile, u.email, NOT u.marketing_opt_out, u.date_of_birth, NOW(), NOW()
FROM users u
WHERE u.role = 'CUSTOMER'
  AND NOT EXISTS (SELECT 1 FROM customer_profiles cp WHERE cp.online_user_id = u.id);

CREATE INDEX idx_customer_profile_mobile ON customer_profiles (mobile);
CREATE INDEX idx_customer_profile_email ON customer_profiles (email);
CREATE INDEX idx_customer_profile_dob ON customer_profiles (date_of_birth);
CREATE INDEX idx_marketing_delivery_campaign_status ON marketing_deliveries (campaign_id, status);
CREATE INDEX idx_marketing_delivery_customer ON marketing_deliveries (customer_profile_id);
