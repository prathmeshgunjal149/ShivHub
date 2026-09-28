-- Existing ShivHub databases created with a native MySQL ENUM may not contain
-- the later-added RAZORPAY value. Store the Java enum name in a VARCHAR so
-- future payment methods do not require a destructive enum migration.
ALTER TABLE offline_bills
    MODIFY COLUMN payment_method VARCHAR(40) NOT NULL;
