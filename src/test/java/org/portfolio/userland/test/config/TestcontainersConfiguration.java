package org.portfolio.userland.test.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Configuration for Testcontainer.
 * Right now, it only configures PostgreSQL database used in tests.
 * <p>The bean returns the shared singleton container ({@link TestPostgres}) instead of creating a new one,
 * so tests importing this configuration (like {@code UserLandApplicationTests}) and the local dev launcher
 * ({@code TestUserLandApplication}) reuse the same pinned {@code postgres:17-alpine} image as the rest of the suite.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
	/**
	 * Expose the singleton PostgreSQL container as a bean so Spring Boot can wire it via @ServiceConnection.
	 * @return Shared PostgreSQL container instance.
	 */
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return TestPostgres.get();
	}
}
