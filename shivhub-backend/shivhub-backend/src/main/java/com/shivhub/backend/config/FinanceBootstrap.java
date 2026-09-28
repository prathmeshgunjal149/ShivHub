package com.shivhub.backend.config;

import com.shivhub.backend.entity.FinanceScheme;
import com.shivhub.backend.repository.FinanceSchemeRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FinanceBootstrap {
    @Bean
    ApplicationRunner financeSchemeDefaults(FinanceSchemeRepository schemes) {
        return arguments -> {
            if (schemes.count() != 0) return;
            save(schemes, "6/2", 6, 2); save(schemes, "8/2", 8, 2);
            save(schemes, "10/3", 10, 3); save(schemes, "12/2", 12, 2);
        };
    }
    private void save(FinanceSchemeRepository schemes, String name, int tenureMonths, int advanceMonths) {
        FinanceScheme scheme = new FinanceScheme(); scheme.setName(name); scheme.setTenureMonths(tenureMonths); scheme.setAdvanceMonths(advanceMonths); scheme.setActive(true); schemes.save(scheme);
    }
}
