package org.portfolio.userland.base;

import org.portfolio.userland.config.MutableClockConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

/**
 * Base annotation for integration tests. Reduces boilerplate and includes commonly used stuff.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest // Boots the application context
@AutoConfigureMockMvc
@Testcontainers // Use containers
@Import(MutableClockConfig.class)
@ActiveProfiles("test")
public @interface IntegrationTest {
}
