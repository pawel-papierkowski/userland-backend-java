package org.portfolio.userland.test;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.config.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * This is a standard integration test designed to verify that your Spring application context starts successfully
 * without any configuration errors.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserLandApplicationTests {
	/** Sanity check. Is anything working at all? */
	@Test
	public void contextLoads() {
	}
}
