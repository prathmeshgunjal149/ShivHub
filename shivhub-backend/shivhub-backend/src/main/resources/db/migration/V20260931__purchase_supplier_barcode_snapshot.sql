-- Additive purchase-line identity snapshot. Existing purchase rows remain valid.
ALTER TABLE purchase_items ADD COLUMN IF NOT EXISTS supplier_barcode VARCHAR(150) NULL;
