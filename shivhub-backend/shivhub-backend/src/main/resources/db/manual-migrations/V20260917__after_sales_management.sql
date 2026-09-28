-- ShivHub After-Sales Management: additive MySQL 8 migration.
-- Run once after the existing migrations. No existing table or column is changed.

CREATE TABLE IF NOT EXISTS service_requests (
  id BIGINT NOT NULL AUTO_INCREMENT,
  request_number VARCHAR(50) NOT NULL,
  request_type VARCHAR(40) NOT NULL,
  status VARCHAR(45) NOT NULL,
  customer_id BIGINT NOT NULL, seller_id BIGINT NULL, shop_id BIGINT NULL, product_id BIGINT NOT NULL,
  order_id BIGINT NULL, order_item_id BIGINT NULL, offline_bill_id BIGINT NULL, offline_bill_item_id BIGINT NULL,
  purchase_serial_id BIGINT NULL, imei1_snapshot VARCHAR(30) NULL, imei2_snapshot VARCHAR(30) NULL,
  serial_number_snapshot VARCHAR(100) NULL, issue_category VARCHAR(80) NULL, customer_issue TEXT NOT NULL,
  seller_diagnosis TEXT NULL, inspection_finding VARCHAR(40) NULL, receiving_condition TEXT NULL, internal_notes TEXT NULL, customer_visible_remarks TEXT NULL,
  warranty_eligible BIT NOT NULL DEFAULT b'0', warranty_start_date DATE NULL, warranty_end_date DATE NULL,
  return_eligible BIT NOT NULL DEFAULT b'0', request_date DATETIME NOT NULL, received_date DATETIME NULL,
  expected_completion_date DATE NULL, completed_date DATETIME NULL, closed_date DATETIME NULL,
  pickup_type VARCHAR(30) NOT NULL, assigned_technician_id BIGINT NULL, rejection_reason TEXT NULL,
  created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id), UNIQUE KEY uk_service_request_number (request_number),
  KEY idx_service_request_customer (customer_id), KEY idx_service_request_seller_status (seller_id, status),
  KEY idx_service_request_order (order_id), KEY idx_service_request_bill (offline_bill_id),
  KEY idx_service_request_serial (purchase_serial_id), KEY idx_service_request_created (created_at)
);

CREATE TABLE IF NOT EXISTS service_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, previous_status VARCHAR(45) NULL,
  new_status VARCHAR(45) NOT NULL, remarks TEXT NULL, changed_by_user_id BIGINT NOT NULL,
  changed_at DATETIME NOT NULL, customer_visible BIT NOT NULL DEFAULT b'0', PRIMARY KEY (id),
  KEY idx_service_history_request_changed (service_request_id, changed_at),
  CONSTRAINT fk_service_history_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS service_attachments (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, attachment_type VARCHAR(30) NOT NULL,
  file_url VARCHAR(2000) NOT NULL, uploaded_by_user_id BIGINT NOT NULL, uploaded_at DATETIME NOT NULL,
  PRIMARY KEY (id), KEY idx_service_attachment_request (service_request_id),
  CONSTRAINT fk_service_attachment_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS service_estimates (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, inspection_charge DECIMAL(14,2) NOT NULL DEFAULT 0,
  parts_amount DECIMAL(14,2) NOT NULL DEFAULT 0, labour_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  discount DECIMAL(14,2) NOT NULL DEFAULT 0, taxable_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  cgst DECIMAL(14,2) NOT NULL DEFAULT 0, sgst DECIMAL(14,2) NOT NULL DEFAULT 0, igst DECIMAL(14,2) NOT NULL DEFAULT 0,
  grand_total DECIMAL(14,2) NOT NULL DEFAULT 0, advance_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  remaining_amount DECIMAL(14,2) NOT NULL DEFAULT 0, customer_approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  approved_at DATETIME NULL, created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL, PRIMARY KEY (id),
  UNIQUE KEY uk_service_estimate_request (service_request_id),
  CONSTRAINT fk_service_estimate_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS service_parts (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, part_name VARCHAR(180) NOT NULL,
  part_number VARCHAR(100) NULL, quantity INT NOT NULL, unit_price DECIMAL(14,2) NOT NULL, gst_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
  total_amount DECIMAL(14,2) NOT NULL, warranty_months INT NULL, PRIMARY KEY (id), KEY idx_service_part_request (service_request_id),
  CONSTRAINT fk_service_part_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS return_replacements (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, action_type VARCHAR(20) NOT NULL,
  old_purchase_serial_id BIGINT NULL, replacement_purchase_serial_id BIGINT NULL, price_difference DECIMAL(14,2) NOT NULL DEFAULT 0,
  additional_payment DECIMAL(14,2) NOT NULL DEFAULT 0, refundable_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  inspection_result TEXT NULL, completed_at DATETIME NULL, PRIMARY KEY (id), UNIQUE KEY uk_return_replacement_request (service_request_id),
  CONSTRAINT fk_return_replacement_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS service_refunds (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, payment_transaction_id BIGINT NULL,
  refund_amount DECIMAL(14,2) NOT NULL, refund_method VARCHAR(40) NOT NULL, refund_status VARCHAR(20) NOT NULL,
  transaction_reference VARCHAR(120) NULL, initiated_at DATETIME NOT NULL, completed_at DATETIME NULL,
  failure_reason TEXT NULL, PRIMARY KEY (id), KEY idx_service_refund_request_status (service_request_id, refund_status),
  CONSTRAINT fk_service_refund_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS service_center_dispatches (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, distributor_id BIGINT NULL,
  service_center_name VARCHAR(180) NOT NULL, dispatch_date DATE NOT NULL, courier_name VARCHAR(100) NULL,
  tracking_number VARCHAR(120) NULL, expected_return_date DATE NULL, received_back_date DATE NULL,
  claim_number VARCHAR(100) NULL, claim_status VARCHAR(50) NULL, claim_amount DECIMAL(14,2) NULL,
  remarks TEXT NULL, credit_note_reference VARCHAR(120) NULL, PRIMARY KEY (id), UNIQUE KEY uk_service_dispatch_request (service_request_id),
  CONSTRAINT fk_service_dispatch_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS after_sales_policies (
  id BIGINT NOT NULL AUTO_INCREMENT, category_id BIGINT NULL, product_id BIGINT NULL, warranty_type VARCHAR(30) NOT NULL DEFAULT 'NONE',
  warranty_months INT NOT NULL DEFAULT 0, returnable BIT NOT NULL DEFAULT b'0', return_window_days INT NOT NULL DEFAULT 0,
  replacement_window_days INT NOT NULL DEFAULT 0, doa_window_days INT NOT NULL DEFAULT 0,
  physical_damage_allowed BIT NOT NULL DEFAULT b'0', liquid_damage_allowed BIT NOT NULL DEFAULT b'0',
  opened_box_return_allowed BIT NOT NULL DEFAULT b'0', change_of_mind_allowed BIT NOT NULL DEFAULT b'0',
  required_evidence TEXT NULL, policy_terms TEXT NULL, active BIT NOT NULL DEFAULT b'1',
  created_by_user_id BIGINT NULL, updated_by_user_id BIGINT NULL, PRIMARY KEY (id),
  KEY idx_after_sales_policy_category (category_id, active), KEY idx_after_sales_policy_product (product_id, active)
);

CREATE TABLE IF NOT EXISTS service_notification_logs (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, customer_id BIGINT NOT NULL,
  notification_type VARCHAR(40) NOT NULL, channel VARCHAR(20) NOT NULL, recipient VARCHAR(300) NOT NULL,
  status VARCHAR(20) NOT NULL, sent_at DATETIME NULL, failure_reason TEXT NULL, PRIMARY KEY (id),
  KEY idx_service_notification_request (service_request_id),
  CONSTRAINT fk_service_notification_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS after_sales_audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, actor_user_id BIGINT NOT NULL,
  action VARCHAR(60) NOT NULL, reason TEXT NOT NULL, created_at DATETIME NOT NULL, PRIMARY KEY (id),
  KEY idx_after_sales_audit_request (service_request_id, created_at),
  CONSTRAINT fk_after_sales_audit_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS after_sales_stock_movements (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, purchase_serial_id BIGINT NULL,
  from_status VARCHAR(40) NULL, to_status VARCHAR(40) NOT NULL, changed_by_user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL, remarks TEXT NULL, PRIMARY KEY (id), KEY idx_after_sales_stock_request (service_request_id, created_at),
  CONSTRAINT fk_after_sales_stock_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);

CREATE TABLE IF NOT EXISTS after_sales_credit_notes (
  id BIGINT NOT NULL AUTO_INCREMENT, service_request_id BIGINT NOT NULL, credit_note_number VARCHAR(80) NOT NULL,
  original_reference VARCHAR(120) NOT NULL, taxable_amount DECIMAL(14,2) NOT NULL, cgst DECIMAL(14,2) NOT NULL,
  sgst DECIMAL(14,2) NOT NULL, igst DECIMAL(14,2) NOT NULL, grand_total DECIMAL(14,2) NOT NULL,
  created_at DATETIME NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_after_sales_credit_note_request (service_request_id),
  UNIQUE KEY uk_after_sales_credit_note_number (credit_note_number),
  CONSTRAINT fk_after_sales_credit_note_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id)
);
