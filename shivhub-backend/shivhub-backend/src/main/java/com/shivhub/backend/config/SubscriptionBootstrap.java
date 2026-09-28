package com.shivhub.backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.shivhub.backend.service.SellerEntitlementService;

@Configuration
public class SubscriptionBootstrap {
    @Bean
    ApplicationRunner subscriptionDefaults(SellerEntitlementService subscriptions) {
        return arguments -> subscriptions.ensureDefaultConfiguration();
    }
}
