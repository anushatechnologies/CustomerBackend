package com.example.project.customer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FlywayConfig {

    /**
     * Automatically repairs the Flyway schema history table before running migrations.
     * This automatically cleans up any failed migration states in MySQL without manual DB intervention.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Running Flyway repair to clean any failed/half-completed migrations...");
            try {
                flyway.repair();
            } catch (Exception e) {
                log.warn("Flyway repair encountered warning: {}", e.getMessage());
            }
            log.info("Running Flyway migrate...");
            flyway.migrate();
            log.info("Flyway migration completed successfully.");
        };
    }
}
