package org.portfolio.userland;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Entry point to Spring Boot application.</p>
 * <p>Note that placement of @SpringBootApplication determines root package to be scanned, including subpackages.</p>
 */
@SpringBootApplication
// This tells GraalVM to preserve the reflection metadata for Flyway's PostgreSQL extension.
@RegisterReflectionForBinding(org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension.class)
public class UserLandApplication {
	/**
	 * Entry point.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(UserLandApplication.class, args);
	}
}
