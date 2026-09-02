package org.portfolio.userland.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * ShedLock configuration.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m") // enable ShedLock
public class ShedLockConfig {
  /**
   * Configures ShedLock. Among other things, we tell it which shedlock table to use.
   * @param dataSource Data source.
   * @param transactionManager Transaction manager.
   * @return Lock provider.
   */
  @Bean
  public LockProvider lockProvider(DataSource dataSource, PlatformTransactionManager transactionManager) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .withTransactionManager(transactionManager) // prevents weird database issues when running multiple tests
            .usingDbTime() // Uses PostgreSQL time, ignoring server clock drift.
            .withTableName("aux.shedlock") // Ensures ShedLock uses correct schema.
            .build()
    );
  }
}
