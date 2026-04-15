package org.portfolio.userland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Entry point to Spring Boot application.</p>
 * <p>Note that placement of @SpringBootApplication determines root package to be scanned, including subpackages.</p>
 * <p>Warning: you need correct variables in run environment.</p>
 * <p>Obligatory for plain email config (if not present, app will crash):</p>
 * <ul>
 *   <li>EMAIL_HOST</li>
 *   <li>EMAIL_USERNAME</li>
 *   <li>EMAIL_PASSWORD</li>
 * </ul>
 * <p>Optional for database (if not present, will use local docker container):</p>
 * <ul>
 *   <li>SPRING_DATASOURCE_URL=jdbc:postgresql://example.postgresql.hosting.org:5432/userland?sslmode=require</li>
 *   <li>SPRING_DATASOURCE_USERNAME=admin</li>
 *   <li>SPRING_DATASOURCE_PASSWORD=FAKE_PASSWORD</li>
 *   <li>SPRING_DOCKER_COMPOSE_ENABLED=false</li>
 * </ul>
 */
@SpringBootApplication
public class UserLandApplication {
	/**
	 * Entry point.
	 * @param args Arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(UserLandApplication.class, args);
	}
}
