package org.portfolio.userland.features.email;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.EmailService;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static ch.martinelli.oss.testcontainers.mailpit.assertions.MailpitAssertions.assertThat;

/**
 * Tests email service for provider 'plain'. Note we do not mock <code>JavaMailSender</code>. Instead, we redirect it
 * to <code>Mailpit</code>.
 */
public class EmailServicePlainTest extends BaseIntegrationTest {
  @Autowired
  private EmailService emailService;

  // Spin up the Mailpit container and auto-wire it to Spring Boot's JavaMailSender.
  @Container
  @ServiceConnection
  static MailpitContainer mailpit = new MailpitContainer();

  /** Clear all emails from pit. */
  @AfterEach
  void tearDown() {
    mailpit.getClient().deleteAllMessages();
  }

  @Test
  public void plainEmailSimple() {
    // Arrange: prepare email request. Note: caller-provided sender must be ignored (security), see assertion below.
    EmailReq emailReq = new EmailReq(
        "plain",
        "pl",
        "tester@test.test",
        List.of("newuser@example.com"),
        List.of(),
        List.of(),
        "",
        "TITLE",
        null,
        null,
        "<p>Content</p>");

    // Act: send email.
    emailService.queueEmail(emailReq);

    // Assert: that email was actually sent, and that configured system sender was used instead of the
    // one provided in the request ("tester@test.test").
    assertThat(mailpit)
        .hasMessages()
        .hasMessageCount(1)
        .hasMessageFrom("pawel.papierkowski.portfolio@gmail.com")
        .hasMessageTo("newuser@example.com")
        .hasMessageWithSubject("[TEST] TITLE"); // We add [TEST] to subject due to app.main.build being TEST
  }
}
