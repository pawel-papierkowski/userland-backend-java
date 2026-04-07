package org.portfolio.userland.backend;

import org.portfolio.userland.UserLandApplication;
import org.portfolio.userland.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * <p>Specialized application entry point for running your backend locally during the development phase.</p>
 * <p>Notes:
 * <ul>
 * <li>Spins up fresh instance PostgreSQL database for you. Data will be lost after you stop application.</li>
 * </ul></p>
 */
public class TestUserLandApplication {
	/**
	 * Entry point.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.from(UserLandApplication::main).with(TestcontainersConfiguration.class).run(args);
	}
}
