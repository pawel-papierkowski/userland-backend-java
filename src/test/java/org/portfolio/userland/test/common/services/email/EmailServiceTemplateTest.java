package org.portfolio.userland.test.common.services.email;

import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.base.BaseIntegrationTest;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;

/**
 * Tests if email templating engine works correctly.
 */
public class EmailServiceTemplateTest extends BaseIntegrationTest {
  // Just to prevent mails being actually sent somewhere.
  @Container
  @ServiceConnection
  static MailpitContainer mailpit = new MailpitContainer();

  @Autowired
  private EmailService emailService;

  @Test
  public void noTemplate() {
    // Arrange: prepare email request.
    EmailReq emailReq = new EmailReq(
        "plain",
        "pl",
        "tester@test.test",
        new String[]{"newuser@example.com"},
        new String[]{},
        new String[]{},
        "",
        "TITLE",
        null,
        null,
        "<p>Content</p>"); // filled, that means templating engine is skipped

    // Act: simulate sending email.
    emailService.sendEmail(emailReq);

    // Assert: TODO somehow check that templating engine was not called?
  }
}
