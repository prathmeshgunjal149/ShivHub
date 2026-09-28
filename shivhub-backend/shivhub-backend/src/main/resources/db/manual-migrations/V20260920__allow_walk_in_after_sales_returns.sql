-- A seller may receive a return for a paid walk-in POS bill that has no online user account.
-- Relaxing this nullability preserves all existing registered-customer links.
ALTER TABLE service_requests MODIFY COLUMN customer_id BIGINT NULL;
