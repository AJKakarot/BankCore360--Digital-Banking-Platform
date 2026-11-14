package com.bankcore360.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Enterprise Testcontainers Integration Test.
 * Validates dynamic spin-up of isolated PostgreSQL and Kafka containers
 * when Docker is available in the CI/CD environment.
 */
class ContainerizedInfrastructureTest {

    @Test
    @DisplayName("Verify PostgreSQL Testcontainer starts cleanly when Docker is active")
    void testPostgreSqlTestcontainer() {
        assumeTrue(isDockerActive(), "Docker daemon is not active; skipping containerized PostgreSQL test");

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.withDatabaseName("bankcore360_test")
                    .withUsername("testuser")
                    .withPassword("testpass")
                    .start();

            assertThat(postgres.isRunning()).isTrue();
            assertThat(postgres.getJdbcUrl()).contains("jdbc:postgresql://");
        }
    }

    @Test
    @DisplayName("Verify Kafka Testcontainer starts cleanly when Docker is active")
    void testKafkaTestcontainer() {
        assumeTrue(isDockerActive(), "Docker daemon is not active; skipping containerized Kafka test");

        try (KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0")) {
            kafka.start();
            assertThat(kafka.isRunning()).isTrue();
            assertThat(kafka.getBootstrapServers()).isNotBlank();
        }
    }

    private boolean isDockerActive() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}

