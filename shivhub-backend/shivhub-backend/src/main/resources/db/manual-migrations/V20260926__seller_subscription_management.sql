-- Additive seller subscription schema. Existing sellers are deliberately not
-- assigned invented approval/trial dates; legacy records remain safe until an
-- administrator chooses an explicit subscription action.

CREATE TABLE IF NOT EXISTS subscription_plans (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL,
    description TEXT NULL,
    price DECIMAL(14,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    billing_interval VARCHAR(20) NOT NULL,
    billing_interval_count INT NOT NULL DEFAULT 1,
    trial_months INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT b'1',
    recommended BIT NOT NULL DEFAULT b'0',
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_subscription_plan_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS subscription_features (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    feature_code VARCHAR(100) NOT NULL,
    feature_name VARCHAR(160) NOT NULL,
    description TEXT NULL,
    category VARCHAR(80) NULL,
    access_type VARCHAR(30) NOT NULL,
    active BIT NOT NULL DEFAULT b'1',
    CONSTRAINT uk_subscription_feature_code UNIQUE (feature_code)
);

CREATE TABLE IF NOT EXISTS plan_features (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    feature_id BIGINT NOT NULL,
    enabled BIT NOT NULL DEFAULT b'1',
    CONSTRAINT uk_plan_feature UNIQUE (plan_id, feature_id),
    CONSTRAINT fk_plan_features_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    CONSTRAINT fk_plan_features_feature FOREIGN KEY (feature_id) REFERENCES subscription_features(id)
);

CREATE TABLE IF NOT EXISTS seller_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    current_plan_id BIGINT NULL,
    trial_start_date DATE NULL,
    trial_end_date DATE NULL,
    subscription_start_date DATE NULL,
    subscription_end_date DATE NULL,
    next_billing_date DATE NULL,
    status VARCHAR(30) NOT NULL,
    auto_renew_enabled BIT NOT NULL DEFAULT b'0',
    payment_gateway VARCHAR(40) NULL,
    gateway_customer_id VARCHAR(120) NULL,
    gateway_subscription_id VARCHAR(120) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_seller_subscription_seller UNIQUE (seller_id),
    CONSTRAINT fk_seller_subscription_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_seller_subscription_plan FOREIGN KEY (current_plan_id) REFERENCES subscription_plans(id),
    INDEX idx_seller_subscription_status (status),
    INDEX idx_seller_subscription_end (subscription_end_date),
    INDEX idx_seller_trial_end (trial_end_date)
);

CREATE TABLE IF NOT EXISTS seller_subscription_entitlements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_subscription_id BIGINT NOT NULL,
    feature_code VARCHAR(100) NOT NULL,
    enabled BIT NOT NULL DEFAULT b'0',
    CONSTRAINT uk_subscription_entitlement UNIQUE (seller_subscription_id, feature_code),
    CONSTRAINT fk_subscription_entitlement_subscription FOREIGN KEY (seller_subscription_id) REFERENCES seller_subscriptions(id),
    INDEX idx_subscription_entitlement_code (feature_code)
);

CREATE TABLE IF NOT EXISTS subscription_payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    seller_subscription_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(40) NULL,
    gateway_order_id VARCHAR(120) NULL,
    gateway_payment_id VARCHAR(120) NULL,
    gateway_signature VARCHAR(255) NULL,
    billing_period_start DATE NULL,
    billing_period_end DATE NULL,
    paid_at DATETIME NULL,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_subscription_gateway_order UNIQUE (gateway_order_id),
    CONSTRAINT uk_subscription_gateway_payment UNIQUE (gateway_payment_id),
    CONSTRAINT fk_subscription_payment_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_subscription_payment_subscription FOREIGN KEY (seller_subscription_id) REFERENCES seller_subscriptions(id),
    CONSTRAINT fk_subscription_payment_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    INDEX idx_subscription_payment_seller_date (seller_id, created_at),
    INDEX idx_subscription_payment_status (payment_status)
);

CREATE TABLE IF NOT EXISTS subscription_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seller_subscription_id BIGINT NOT NULL,
    actor_user_id BIGINT NULL,
    event_type VARCHAR(80) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    reason TEXT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_subscription_audit_subscription FOREIGN KEY (seller_subscription_id) REFERENCES seller_subscriptions(id),
    INDEX idx_subscription_audit_subscription_date (seller_subscription_id, created_at)
);
