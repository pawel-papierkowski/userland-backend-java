package org.portfolio.userland.features.email;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.email.services.providers.EmailProviderFactory;
import org.portfolio.userland.features.email.services.providers.IntEmailProvider;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests email provider calls.
 */
public class EmailServiceProviderTest extends BaseIntegrationTest {
  @Autowired
  private EmailProviderFactory emailProviderFactory;

  /** Name of default provider. */
  @Value("${app.email.providers.default}")
  private String defaultProvider;

  @Test
  public void useDefaultProvider() {
    // Act: request default email provider
    IntEmailProvider emailProvider = emailProviderFactory.getProvider(null);

    // Assert: check which provider was called.
    assertThat(emailProvider).isNotNull();
    assertThat(emailProvider.getProviderName()).isEqualTo(defaultProvider);
  }
}
