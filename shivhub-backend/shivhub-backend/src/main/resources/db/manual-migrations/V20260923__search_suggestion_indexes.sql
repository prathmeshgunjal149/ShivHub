-- Additive indexes for the bounded autocomplete endpoints.  Run once on MySQL before
-- enabling high-volume search; no existing rows, columns or contracts are changed.
CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products (brand);
CREATE INDEX IF NOT EXISTS idx_products_model ON products (model);
CREATE INDEX IF NOT EXISTS idx_products_seller_sku_search ON products (seller_sku);
CREATE INDEX IF NOT EXISTS idx_products_category_search ON products (category);
CREATE INDEX IF NOT EXISTS idx_customer_profiles_name ON customer_profiles (name);
CREATE INDEX IF NOT EXISTS idx_users_name ON users (name);
CREATE INDEX IF NOT EXISTS idx_users_business_name ON users (business_name);
CREATE INDEX IF NOT EXISTS idx_distributors_business_name ON distributors (business_name);
CREATE INDEX IF NOT EXISTS idx_distributors_mobile ON distributors (mobile);
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories (name);
CREATE INDEX IF NOT EXISTS idx_sub_categories_name ON sub_categories (name);
