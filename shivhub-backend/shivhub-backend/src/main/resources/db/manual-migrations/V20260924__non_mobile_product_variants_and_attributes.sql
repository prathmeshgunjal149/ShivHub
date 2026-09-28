-- MySQL 8: run against the selected ShivHub schema after taking a backup.
-- Additive, repeatable column guards (MySQL does not support ADD COLUMN IF NOT EXISTS).
-- Existing rows, prices, mobile fields and IMEI tables are preserved.
DROP PROCEDURE IF EXISTS shivhub_add_variant_column;
DELIMITER $$
CREATE PROCEDURE shivhub_add_variant_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN column_definition TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = target_table AND column_name = target_column) THEN
    SET @variant_ddl = CONCAT('ALTER TABLE `', target_table, '` ADD COLUMN `', target_column, '` ', column_definition);
    PREPARE variant_statement FROM @variant_ddl;
    EXECUTE variant_statement;
    DEALLOCATE PREPARE variant_statement;
  END IF;
END$$
DELIMITER ;
CALL shivhub_add_variant_column('category_specification_templates', 'options_json', 'TEXT NULL');
CALL shivhub_add_variant_column('category_specification_templates', 'filterable', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL shivhub_add_variant_column('category_specification_templates', 'variant_enabled', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL shivhub_add_variant_column('products', 'variants_enabled', 'BOOLEAN NOT NULL DEFAULT FALSE');
CREATE TABLE IF NOT EXISTS product_variants (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  variant_sku VARCHAR(100) NULL,
  barcode VARCHAR(80) NULL,
  attributes_json TEXT NOT NULL,
  attribute_signature VARCHAR(500) NOT NULL,
  selling_price_including_gst DECIMAL(12,2) NOT NULL,
  compare_at_price DECIMAL(12,2) NULL,
  stock_quantity INT NOT NULL DEFAULT 0,
  reserved_quantity INT NOT NULL DEFAULT 0,
  image_url VARCHAR(1000) NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  reorder_threshold INT NULL,
  version BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uk_product_variant_signature UNIQUE (product_id, attribute_signature),
  CONSTRAINT uk_product_variant_barcode UNIQUE (barcode),
  INDEX idx_product_variant_product_active (product_id, active),
  INDEX idx_product_variant_barcode (barcode),
  INDEX idx_product_variant_sku (variant_sku)
);
CALL shivhub_add_variant_column('cart_items', 'product_variant_id', 'BIGINT NULL');
CALL shivhub_add_variant_column('cart_items', 'selection_key', 'BIGINT NOT NULL DEFAULT 0');
CALL shivhub_add_variant_column('order_items', 'product_variant_id', 'BIGINT NULL');
CALL shivhub_add_variant_column('order_items', 'selected_attributes', 'TEXT NULL');
CALL shivhub_add_variant_column('order_items', 'delivery_estimate_text', 'VARCHAR(500) NULL');
CALL shivhub_add_variant_column('order_items', 'delivery_distance_km', 'DECIMAL(10,2) NULL');
CALL shivhub_add_variant_column('order_items', 'delivery_rule_id', 'BIGINT NULL');
CALL shivhub_add_variant_column('offline_bill_items', 'product_variant_id', 'BIGINT NULL');
CALL shivhub_add_variant_column('offline_bill_items', 'selected_attributes', 'TEXT NULL');
CALL shivhub_add_variant_column('stock_movements', 'product_variant_id', 'BIGINT NULL');
CALL shivhub_add_variant_column('service_requests', 'product_variant_id', 'BIGINT NULL');
CALL shivhub_add_variant_column('after_sales_stock_movements', 'product_variant_id', 'BIGINT NULL');
CALL shivhub_add_variant_column('after_sales_stock_movements', 'quantity', 'INT NULL');
CALL shivhub_add_variant_column('after_sales_stock_movements', 'stock_restored', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL shivhub_add_variant_column('customer_addresses', 'latitude', 'DECIMAL(10,7) NULL');
CALL shivhub_add_variant_column('customer_addresses', 'longitude', 'DECIMAL(10,7) NULL');
CALL shivhub_add_variant_column('customer_addresses', 'geocoded_address_hash', 'VARCHAR(64) NULL');
CALL shivhub_add_variant_column('users', 'business_latitude', 'DECIMAL(10,7) NULL');
CALL shivhub_add_variant_column('users', 'business_longitude', 'DECIMAL(10,7) NULL');
CALL shivhub_add_variant_column('users', 'business_geocoded_address_hash', 'VARCHAR(64) NULL');
CREATE TABLE IF NOT EXISTS delivery_rule_configuration (id BIGINT NOT NULL PRIMARY KEY);
CREATE TABLE IF NOT EXISTS delivery_distance_rules (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  product_scope VARCHAR(30) NOT NULL DEFAULT 'MOBILE_ONLY',
  minimum_distance_km DECIMAL(10,3) NOT NULL,
  maximum_distance_km DECIMAL(10,3) NULL,
  estimated_delivery_text VARCHAR(500) NOT NULL,
  estimated_minutes INT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  priority INT NOT NULL DEFAULT 0,
  delivery_charge DECIMAL(12,2) NOT NULL DEFAULT 0,
  service_available BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NULL,
  INDEX idx_delivery_scope_active (product_scope, active)
);
DROP PROCEDURE shivhub_add_variant_column;
-- VariantCartSchemaUpgrade performs the guarded cart uniqueness upgrade at startup:
-- (customer_id, product_id) -> (customer_id, product_id, selection_key).
-- It changes only indexes; cart rows are retained. Do not delete cart rows manually.
-- ProductExtensionSeeder creates templates for EXISTING non-mobile subcategories only
-- when unconfigured, and seeds delivery rules only when that table is empty.
