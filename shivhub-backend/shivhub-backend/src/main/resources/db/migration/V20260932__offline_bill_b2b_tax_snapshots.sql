ALTER TABLE offline_bills ADD COLUMN IF NOT EXISTS customer_legal_name VARCHAR(180) NULL;
ALTER TABLE offline_bills ADD COLUMN IF NOT EXISTS customer_trade_name VARCHAR(180) NULL;
ALTER TABLE offline_bills ADD COLUMN IF NOT EXISTS place_of_supply VARCHAR(80) NULL;
ALTER TABLE offline_bills ADD COLUMN IF NOT EXISTS customer_tax_type VARCHAR(8) NULL;
