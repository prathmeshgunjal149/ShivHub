-- Optional barcode leaves all existing product records and mobile flows unchanged.
ALTER TABLE products ADD COLUMN IF NOT EXISTS barcode VARCHAR(80) NULL AFTER seller_sku;
CREATE UNIQUE INDEX IF NOT EXISTS uk_products_barcode ON products (barcode);
CREATE INDEX IF NOT EXISTS idx_products_seller_sku ON products (seller_id, seller_sku);
CREATE INDEX IF NOT EXISTS idx_purchase_item_serials_imei1 ON purchase_item_serials (imei1);
CREATE INDEX IF NOT EXISTS idx_purchase_item_serials_imei2 ON purchase_item_serials (imei2);
CREATE INDEX IF NOT EXISTS idx_purchase_item_serials_serial_number ON purchase_item_serials (serial_number);
CREATE INDEX IF NOT EXISTS idx_purchase_item_serials_status ON purchase_item_serials (status);
