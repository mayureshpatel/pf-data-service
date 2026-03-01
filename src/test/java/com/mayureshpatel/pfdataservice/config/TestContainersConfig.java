package com.mayureshpatel.pfdataservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Modern Testcontainers configuration for Spring Boot 3.1+.
 * Using @ServiceConnection allows Spring to automatically manage the JDBC properties.
 * When used with @Import in test classes, Spring manages the lifecycle of the container
 * alongside the ApplicationContext, ensuring the container stays alive as long as the context is cached.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("personal_finance_test")
                .withUsername("test")
                .withPassword("test")
                .withTmpFs(java.util.Collections.singletonMap("/var/lib/postgresql/data", "rw"));
    }
}
