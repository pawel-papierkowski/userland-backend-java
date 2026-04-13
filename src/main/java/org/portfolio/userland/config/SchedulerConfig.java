package org.portfolio.userland.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * General scheduler configuration. We need to ensure schedulers do not run in tests.
 * If you want to actually test scheduler, annotate test class with
 * @TestPropertySource(properties = "app.scheduling.enabled=true")
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    value = "app.scheduling.enabled", // this property will exist only in tests
    havingValue = "true",
    matchIfMissing = true // ensures it runs normally in production
)
public class SchedulerConfig {
}
