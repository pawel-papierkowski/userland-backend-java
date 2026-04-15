package org.portfolio.userland.test.common.services.email;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.base.BaseIntegrationTest;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.email.exception.UnknownEmailProviderException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests email service in general.
 */
public class EmailServiceTest extends BaseIntegrationTest {
  @Autowired
  private EmailService emailService;

  @Test
  public void errUnknownEmailProvider() {
    // Arrange: prepare email request.
    EmailReq emailReq = new EmailReq(
        "invalid_provider",
        "en",
        "tester@test.test",
        List.of("newuser@example.com"),
        List.of(),
        List.of(),
        "",
        "TITLE",
        null,
        null,
        "<p>Content</p>");

    // Act & Assert: simulate sending email, fail due to unknown provider.
    UnknownEmailProviderException actualEx = assertThrows(
        UnknownEmailProviderException.class,
        () -> emailService.sendEmail(emailReq)
    );

    // Assert: exception details.
    assertThat(actualEx.getDetail()).as("Exception detail is wrong").isEqualTo("Email provider 'invalid_provider' does not exist.");
  }
}
