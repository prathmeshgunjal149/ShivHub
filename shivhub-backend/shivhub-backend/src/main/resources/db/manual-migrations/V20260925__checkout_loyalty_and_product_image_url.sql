-- MySQL 8+ additive/non-destructive migration.
-- Run against the selected ShivHub schema before the matching application release.
-- Existing rows, product images, and mobile flows are preserved.

SET @schema_name = DATABASE();

-- Supports external CDN image URLs up to the application's validated 2,000 characters.
SET @has_product_images = (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = @schema_name AND table_name = 'product_images'
);
SET @image_url_ddl = IF(@has_product_images = 1,
    'ALTER TABLE product_images MODIFY COLUMN image_url VARCHAR(2048) NOT NULL',
    'SELECT 1');
PREPARE image_url_stmt FROM @image_url_ddl;
EXECUTE image_url_stmt;
DEALLOCATE PREPARE image_url_stmt;

SET @has_loyalty_points_redeemed = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'orders' AND column_name = 'loyalty_points_redeemed'
);
SET @loyalty_points_ddl = IF(@has_loyalty_points_redeemed = 0,
    'ALTER TABLE orders ADD COLUMN loyalty_points_redeemed BIGINT NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE loyalty_points_stmt FROM @loyalty_points_ddl;
EXECUTE loyalty_points_stmt;
DEALLOCATE PREPARE loyalty_points_stmt;

SET @has_loyalty_discount = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'orders' AND column_name = 'loyalty_discount'
);
SET @loyalty_discount_ddl = IF(@has_loyalty_discount = 0,
    'ALTER TABLE orders ADD COLUMN loyalty_discount DECIMAL(12,2) NOT NULL DEFAULT 0.00',
    'SELECT 1');
PREPARE loyalty_discount_stmt FROM @loyalty_discount_ddl;
EXECUTE loyalty_discount_stmt;
DEALLOCATE PREPARE loyalty_discount_stmt;
