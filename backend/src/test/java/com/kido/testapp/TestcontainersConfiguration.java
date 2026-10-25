package com.kido.testapp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Throwaway Postgres for tests (design.md D7). Same engine and Flyway migrations as the app,
 * separate instance so tests never touch the dev database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Keep in sync with docker-compose.yml
    static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE);
    }
}
