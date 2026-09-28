ALTER TABLE offline_bills ADD COLUMN IF NOT EXISTS customer_profile_id BIGINT NULL;
CREATE INDEX IF NOT EXISTS idx_offline_bill_customer_profile ON offline_bills (customer_profile_id);
