-- Additive Finance / EMI schema for MySQL 8+. Existing bills, inventory, IMEI and payment records are untouched.
-- Hibernate update is enabled locally, but production deployments should apply this file before the release.

SET @schema_name = DATABASE();
SET @has_finance_provider = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'offline_bills' AND column_name = 'finance_provider_name'
);
SET @finance_provider_ddl = IF(@has_finance_provider = 0,
    'ALTER TABLE offline_bills ADD COLUMN finance_provider_name VARCHAR(150) NULL',
    'SELECT 1');
PREPARE finance_provider_stmt FROM @finance_provider_ddl;
EXECUTE finance_provider_stmt;
DEALLOCATE PREPARE finance_provider_stmt;

CREATE TABLE IF NOT EXISTS finance_companies (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact VARCHAR(500) NULL,
    active BIT NOT NULL DEFAULT b'1',
    approved_by BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_finance_company_name UNIQUE (name),
    INDEX idx_finance_company_active_name (active, name)
);

CREATE TABLE IF NOT EXISTS finance_schemes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    company_id BIGINT NULL,
    tenure_months INT NOT NULL,
    advance_months INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1',
    CONSTRAINT uk_finance_scheme_name UNIQUE (name),
    CONSTRAINT fk_finance_scheme_company FOREIGN KEY (company_id) REFERENCES finance_companies(id),
    INDEX idx_finance_scheme_company_active (company_id, active)
);

CREATE TABLE IF NOT EXISTS finance_company_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    requested_by BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    contact VARCHAR(500) NULL,
    remarks VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    review_reason VARCHAR(1000) NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_finance_company_request_user_status (requested_by, status),
    INDEX idx_finance_company_request_status (status)
);

CREATE TABLE IF NOT EXISTS finance_sales (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    scheme_name VARCHAR(100) NOT NULL,
    tenure_months INT NOT NULL,
    advance_months INT NOT NULL DEFAULT 0,
    loan_number VARCHAR(150) NULL,
    full_sale_amount DECIMAL(12,2) NOT NULL,
    downpayment DECIMAL(12,2) NOT NULL,
    processing_charges DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    dbd_charges DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    other_charges DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    deduction DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    adjustment DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    expected_disbursement DECIMAL(12,2) NOT NULL,
    actual_disbursement DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    settlement_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    settlement_date DATE NULL,
    settlement_reference VARCHAR(150) NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL,
    active BIT NOT NULL DEFAULT b'1',
    summary_sent BIT NOT NULL DEFAULT b'0',
    summary_sent_at DATETIME NULL,
    remarks VARCHAR(1000) NULL,
    version BIGINT NULL,
    CONSTRAINT uk_finance_sale_bill UNIQUE (bill_id),
    CONSTRAINT fk_finance_sale_bill FOREIGN KEY (bill_id) REFERENCES offline_bills(id),
    CONSTRAINT fk_finance_sale_company FOREIGN KEY (company_id) REFERENCES finance_companies(id),
    INDEX idx_finance_sale_loan (loan_number),
    INDEX idx_finance_sale_status (settlement_status),
    INDEX idx_finance_sale_active_summary (active, summary_sent)
);

CREATE TABLE IF NOT EXISTS emi_installments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    finance_sale_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    month_label VARCHAR(40) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reminder_sent BIT NOT NULL DEFAULT b'0',
    reminder_sent_at DATETIME NULL,
    failure_message VARCHAR(250) NULL,
    CONSTRAINT uk_emi_installment_number UNIQUE (finance_sale_id, installment_number),
    CONSTRAINT fk_emi_installment_sale FOREIGN KEY (finance_sale_id) REFERENCES finance_sales(id),
    INDEX idx_emi_due_reminder (due_date, reminder_sent)
);

CREATE TABLE IF NOT EXISTS finance_audit (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    finance_sale_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    action VARCHAR(40) NULL,
    details VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_finance_audit_sale FOREIGN KEY (finance_sale_id) REFERENCES finance_sales(id),
    INDEX idx_finance_audit_sale_created (finance_sale_id, created_at)
);
