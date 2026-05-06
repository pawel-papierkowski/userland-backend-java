package org.portfolio.userland.features.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.EmailService;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Tests email service for provider 'resend'.
 */
public class EmailServiceResendTest extends BaseIntegrationTest {
  @Autowired
  private EmailService emailService;

  @MockitoBean
  private Resend resendMock; // We do not want to actually send email.

  @Test
  public void resendEmailSimple() throws ResendException {
    // Arrange: setup Resend mock.
    CreateEmailResponse mockResponse = new CreateEmailResponse();
    mockResponse.setId("mock_resend_id_789");

    // Resend uses chaining like this: resend.emails().send(params);
    // So we need to mock response for both resend.emails() and emails().send().
    Emails emailsMock = mock(Emails.class);
    when(resendMock.emails()).thenReturn(emailsMock);
    when(emailsMock.send(any(CreateEmailOptions.class))).thenReturn(mockResponse);

    // Arrange: prepare email request.
    EmailReq emailReq = new EmailReq(
        "resend",
        "pl",
        "tester@test.test",
        List.of("delivered@resend.dev"),
        List.of(),
        List.of(),
        "",
        "TITLE",
        null,
        null,
        "<p>Content</p>");

    // Act: simulate sending email.
    emailService.sendEmail(emailReq);

    // Assert: that Resend API was called.
    verify(emailsMock, times(1)).send(any(CreateEmailOptions.class));
  }
}
