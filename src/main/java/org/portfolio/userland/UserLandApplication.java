package org.portfolio.userland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p>Entry point to Spring Boot application.</p>
 * <p>Note that placement of <code>@SpringBootApplication</code> determines root package to be scanned, including subpackages.</p>
 * <p>Warning: you need correct variables in run environment.</p>
 * <p>Obligatory for plain email config (if not present, app will crash):</p>
 * <ul>
 *   <li><code>EMAIL_HOST</code></li>
 *   <li><code>EMAIL_USERNAME</code></li>
 *   <li><code>EMAIL_PASSWORD</code></li>
 * </ul>
 * <p>Optional for database (if not present, will use local docker container):</p>
 * <ul>
 *   <li><code>SPRING_DATASOURCE_URL</code>=jdbc:postgresql://example.postgresql.hosting.org:5432/userland?sslmode=require</li>
 *   <li><code>SPRING_DATASOURCE_USERNAME</code>=admin</li>
 *   <li><code>SPRING_DATASOURCE_PASSWORD</code>=SOME_PASSWORD</li>
 *   <li><code>SPRING_DOCKER_COMPOSE_ENABLED</code>=false</li>
 * </ul>
 * <p>See <code>README.md</code> for full list of variables used by system.</p>
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
