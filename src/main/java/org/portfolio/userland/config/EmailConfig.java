package org.portfolio.userland.config;

import com.resend.Resend;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.exception.SystemMisconfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Email configuration. Provides <code>Resend</code> bean.
 */
@Configuration
public class EmailConfig {
  /** Resend's API key. */
  @Value("${app.email.providers.resend.api-key}")
  private String resendApiKey;

  /**
   * Resolves Resend instance.
   * @return New Resend instance.
   */
  @Bean
  public Resend resend() {
    if (StringUtils.isEmpty(resendApiKey)) throw new SystemMisconfigurationException("Resend API key is not present!");
    return new Resend(resendApiKey);
  }
}
