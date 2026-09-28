package com.shivhub.backend.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class CampaignAsyncConfig {
    @Bean("campaignEmailExecutor")
    Executor campaignEmailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(4); executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("campaign-email-"); executor.initialize();
        return executor;
    }
}
