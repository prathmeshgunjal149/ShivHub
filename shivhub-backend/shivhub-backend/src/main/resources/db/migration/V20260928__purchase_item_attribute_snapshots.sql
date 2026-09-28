-- Additive snapshot for configured non-mobile purchase specifications.
SET @has_column := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'purchase_items' AND column_name = 'attributes_json'
);
SET @ddl := IF(@has_column = 0,
    'ALTER TABLE purchase_items ADD COLUMN attributes_json TEXT NULL',
    'SELECT 1');
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;
