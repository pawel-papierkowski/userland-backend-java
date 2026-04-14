package org.portfolio.userland.base;

import org.portfolio.userland.config.MutableClockConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

/**
 * Base annotation for integration tests. Reduces boilerplate and includes commonly used stuff.
 * Note: BaseIntegrationTest handles container manually.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AnyTest
@SpringBootTest // Boots the application context
@AutoConfigureMockMvc
@Testcontainers // Use containers
@Import(MutableClockConfig.class)
public @interface IntegrationTest {
}
