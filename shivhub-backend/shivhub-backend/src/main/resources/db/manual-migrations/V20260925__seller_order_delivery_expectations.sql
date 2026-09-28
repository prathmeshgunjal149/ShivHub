-- MySQL 8: run after V20260924. Additive and safe for existing orders.
-- Existing order history is preserved; rows are created only for new estimates or manual updates.
CREATE TABLE IF NOT EXISTS order_delivery_expectations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  seller_name VARCHAR(160) NULL,
  expected_delivery_at DATETIME NOT NULL,
  customer_message VARCHAR(500) NULL,
  updated_by_role VARCHAR(20) NOT NULL,
  updated_at DATETIME NOT NULL,
  version BIGINT NULL,
  CONSTRAINT uk_order_delivery_expectation_seller UNIQUE (order_id, seller_id),
  INDEX idx_order_delivery_expectation_order (order_id),
  INDEX idx_order_delivery_expectation_seller (seller_id, expected_delivery_at)
);
