package org.portfolio.userland.common.services.email;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static ch.martinelli.oss.testcontainers.mailpit.assertions.MailpitAssertions.assertThat;

/**
 * Tests email service for provider 'plain'.
 */
public class EmailServicePlainTest extends BaseIntegrationTest {
  @Autowired
  private EmailService emailService;

  // Spin up the Mailpit container and auto-wire it to Spring Boot's JavaMailSender.
  @Container
  @ServiceConnection
  static MailpitContainer mailpit = new MailpitContainer();

  @AfterEach
  void tearDown() {
    mailpit.getClient().deleteAllMessages();
  }

  @Test
  public void plainEmailSimple() {
    // Arrange: prepare email request.
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

    // Act: simulate sending email.
    emailService.sendEmail(emailReq);

    // Assert: that email was actually sent.
    assertThat(mailpit)
        .hasMessages()
        .hasMessageCount(1)
        .hasMessageFrom("tester@test.test")
        .hasMessageTo("newuser@example.com")
        .hasMessageWithSubject("[TEST] TITLE"); // We add [TEST] to subject due to app.main.build being TEST
  }

  @Test
  public void plainEmailDefault() {
    // Arrange: prepare email request.
    EmailReq emailReq = new EmailReq(
        null, // will use default provider, and it happens to be 'plain'
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

    // Act: simulate sending email.
    emailService.sendEmail(emailReq);

    // Assert: that email was actually sent.
    assertThat(mailpit)
        .hasMessages()
        .hasMessageCount(1)
        .hasMessageFrom("tester@test.test")
        .hasMessageTo("newuser@example.com")
        .hasMessageWithSubject("[TEST] TITLE");
  }
}
