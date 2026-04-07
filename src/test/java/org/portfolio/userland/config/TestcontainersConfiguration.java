package org.portfolio.userland.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuration for Testcontainer.
 * Right now, it only configures PostgreSQL database used in tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
	}
}
