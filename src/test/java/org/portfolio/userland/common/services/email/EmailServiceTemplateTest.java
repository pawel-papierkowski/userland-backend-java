package org.portfolio.userland.common.services.email;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.email.providers.EmailProviderFactory;
import org.portfolio.userland.common.services.email.providers.PlainEmailProvider;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thymeleaf.exceptions.TemplateInputException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Tests if email templating engine works correctly.
 * <p>Note: we cannot spy on real template engine, if you do <code>@MockitoSpyBean private TemplateEngine templateEngine;</code>,
 * running entire test suite with coverage will fail on this file.</p>
 */
public class EmailServiceTemplateTest extends BaseIntegrationTest {
  @MockitoBean
  private EmailProviderFactory emailProviderFactory; // We do not want to actually send email.
  @MockitoBean
  private PlainEmailProvider emailProvider; // Fake provider.

  @Autowired
  private EmailService emailService;

  //

  @Test
  public void noTemplate() {
    // If messageHtml is already provided, call to templating engine is skipped.
    // Arrange: Tell our mock factory to return our mock provider so the code doesn't throw a NullPointerException.
    when(emailProviderFactory.getProvider(anyString())).thenReturn(emailProvider);

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
        "<p>Content</p>"); // filled, that means templating engine is skipped

    // Act: simulate sending email.
    emailService.sendEmail(emailReq);

    // Assert: Verify the email was actually sent to the provider.
    verify(emailProvider).send(emailReq);
    // We know templating engine was not run, because it would throw exception due to null template name.
  }

  @Test
  public void templateSimple() {
    // Arrange: Tell our mock factory to return our mock provider so the code doesn't throw a NullPointerException.
    when(emailProviderFactory.getProvider(anyString())).thenReturn(emailProvider);

    // Arrange: prepare template parameters and email request.
    Map<String, Object> params = Maps.newHashMap();
    params.put("var", "VARIABLE_VALUE");
    EmailReq emailReq = new EmailReq(
        "plain",
        "pl",
        "tester@test.test",
        List.of("newuser@example.com"),
        List.of(),
        List.of(),
        "",
        "TITLE",
        "test/simple",
        params,
        null);

    // Act: simulate sending email.
    emailService.sendEmail(emailReq);

    // Assert: Verify the email was actually sent to the provider.
    ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
    verify(emailProvider).send(captor.capture());

    EmailReq processedReq = captor.getValue();
    assertThat(processedReq.messageHtml()).isNotNull(); // ensure templating engine actually filled messageHtml

    // Verify the HTML contains the exact text injected by the template engine in correct language.
    assertThat(processedReq.messageHtml())
        .contains("To jest tytuł emaila")
        .contains("To jest zawartość emaila. Zmienna: VARIABLE_VALUE.");
  }

  //

  @Test
  public void errUnknownTemplate() {
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
        "unknown",
        null,
        null);

    // Act & Assert: simulate sending email, fail due to unknown template.
    TemplateInputException actualEx = assertThrows(TemplateInputException.class, () -> emailService.sendEmail(emailReq));
    assertThat(actualEx.getMessage()).as("Exception message is wrong").isEqualTo("Error resolving template [unknown], template might not exist or might not be accessible by any of the configured Template Resolvers");

    // Assert: Verify the email was NOT sent to the provider.
    verifyNoInteractions(emailProvider);
  }
}
