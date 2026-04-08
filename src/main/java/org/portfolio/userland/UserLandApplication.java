package org.portfolio.userland;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Entry point to Spring Boot application.</p>
 * <p>Note that placement of @SpringBootApplication determines root package to be scanned, including subpackages.</p>
 */
@SpringBootApplication
@RegisterReflectionForBinding({ // To prevent GraalVM crashes.
		org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension.class,
		org.flywaydb.core.internal.command.clean.CleanModeConfigurationExtension.class,
		org.flywaydb.core.internal.configuration.extensions.DeployScriptFilenameConfigurationExtension.class,
		org.flywaydb.core.internal.logging.log4j2.Log4j2LogCreator.class
})
public class UserLandApplication {
	/**
	 * Entry point.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(UserLandApplication.class, args);
	}
}
