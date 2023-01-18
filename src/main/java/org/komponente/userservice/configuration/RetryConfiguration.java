package org.komponente.userservice.configuration;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfiguration {

    @Bean
    public Retry rentalServiceRetry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(10)
                .waitDuration(java.time.Duration.ofMillis(1000))
                .build();
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        return retryRegistry.retry("rentalServiceRetry");
    }
}
