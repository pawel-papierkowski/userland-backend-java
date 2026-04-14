package org.portfolio.userland.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * General configuration for application.
 */
@Configuration
@EnableAsync
public class AppConfig {
  /**
   * Defines task executor dedicated to email sending. Note it is used by intermediate class, not by EmailService itself.
   * <p>Example: UserRegisterService -> UserEmailService (here is new thread via event) -> EmailService.</p>
   * @return Executor.
   */
  @Bean(name = "emailTaskExecutor")
  public Executor emailTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2); // Minimum number of threads kept alive.
    executor.setMaxPoolSize(10); // Maximum number of threads allowed to run concurrently.
    executor.setQueueCapacity(50); // How many emails can wait in line before Spring rejects them.
    executor.setThreadNamePrefix("EmailSender-"); // Helps you easily identify these threads in your server logs.
    executor.initialize();
    return executor;
  }
}
