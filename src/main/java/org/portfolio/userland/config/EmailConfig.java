package org.portfolio.userland.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Email configuration.
 */
@Configuration
public class EmailConfig {
  /** Resend's API key. */
  @Value("${app.email.providers.resend.apiKey}")
  private String resendApiKey;

  @Bean
  public Resend resend() {
    return new Resend(resendApiKey);
  }
}
