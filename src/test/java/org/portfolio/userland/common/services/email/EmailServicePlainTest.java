package org.portfolio.userland.common.services.email;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
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
  // Spin up the Mailpit container and auto-wire it to Spring Boot's JavaMailSender.
  @Container
  @ServiceConnection
  static MailpitContainer mailpit = new MailpitContainer();

  @Autowired
  private EmailService emailService;

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
        .hasMessageWithSubject("TITLE");
  }
}
