package org.portfolio.userland.test.common.services.email;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.base.BaseIntegrationTest;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests email service.
 */
public class EmailServiceTest extends BaseIntegrationTest {
  @Autowired
  private EmailService emailService;

  @Test
  public void simpleEmailSend() {
    // Simulate sending simple email.

    // Arrange: prepare email request.
    EmailReq emailReq = new EmailReq("plain", "tester@test.test", new String[]{}, new String[]{}, new String[]{}, "", "TITLE", null, null, "<p>Content</p>");

    // Act: simulate sending email.
    //emailService.sendEmail(emailReq); // TODO: handle Mailpit

    // Assert: that email was actually sent.
  }
}
