-- Additive WhatsApp notification/registration verification support.
ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp_opt_in BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS whatsapp_otp_verifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    sent_at DATETIME NOT NULL,
    verified_at DATETIME NULL,
    CONSTRAINT fk_whatsapp_otp_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_whatsapp_otp_user ON whatsapp_otp_verifications(user_id);

CREATE TABLE IF NOT EXISTS whatsapp_delivery_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(30) NOT NULL,
    event_key VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_status VARCHAR(60) NULL,
    failure_reason TEXT NULL,
    sent_at DATETIME NULL,
    created_at DATETIME NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_whatsapp_delivery_key ON whatsapp_delivery_logs(recipient, event_key);
CREATE INDEX IF NOT EXISTS idx_whatsapp_delivery_status ON whatsapp_delivery_logs(status, created_at);
