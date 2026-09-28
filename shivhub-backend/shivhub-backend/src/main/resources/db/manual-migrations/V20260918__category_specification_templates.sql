CREATE TABLE IF NOT EXISTS category_specification_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    subcategory_id BIGINT NULL,
    specification_key VARCHAR(80) NOT NULL,
    display_label VARCHAR(120) NOT NULL,
    input_type VARCHAR(20) NOT NULL,
    required_field BIT NOT NULL DEFAULT 0,
    display_order INT NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_spec_template_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_spec_template_subcategory FOREIGN KEY (subcategory_id) REFERENCES sub_categories(id),
    INDEX idx_spec_template_category (category_id, subcategory_id, active, display_order)
);
