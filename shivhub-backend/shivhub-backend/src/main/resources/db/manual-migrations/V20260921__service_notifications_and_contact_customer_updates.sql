-- A walk-in POS return may have an email on its bill but no registered user account.
-- Keep the notification audit record while allowing that account link to remain null.
ALTER TABLE service_notification_logs MODIFY COLUMN customer_id BIGINT NULL;

-- Customer-visible reply is separate from internal admin notes and is safe to email.
ALTER TABLE customer_enquiries ADD COLUMN customer_response TEXT NULL AFTER internal_notes;
