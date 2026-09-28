package com.shivhub.backend.entity;
import jakarta.persistence.*;
/** Singleton row serializes configuration changes across application instances. */
@Entity @Table(name="delivery_rule_configuration")
public class DeliveryRuleConfiguration {
    @Id private Long id;
    protected DeliveryRuleConfiguration() { }
    public DeliveryRuleConfiguration(Long id) { this.id=id; }
}
