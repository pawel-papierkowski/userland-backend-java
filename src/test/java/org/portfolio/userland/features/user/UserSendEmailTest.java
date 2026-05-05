package org.portfolio.userland.features.user;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.events.*;
import org.portfolio.userland.features.user.services.UserHelperService;
import org.portfolio.userland.features.user.services.UserSendEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies if UserEmailService constructs correct EmailReq and calls emailService based on event data.
 * Note: methods of this service are called from appropriate events.
 */
public class UserSendEmailTest extends BaseUserTest {
  @Autowired
  private UserSendEmailService userSendEmailService;

  /** Name of system. */
  @Value("${app.main.name}")
  protected String systemName;
  /** Base frontend address. */
  @Value("${app.main.www}")
  private String frontendWww;
  /** Who sends emails? */
  @Value("${app.email.sender}")
  private String emailSender;
  @Autowired
  private UserHelperService userHelperService;

  @Test
  public void sendRegistrationEmail() {
    // Arrange: event data.
    UserRegisteredEvent event = new UserRegisteredEvent(
        1L,
        "Jan Kowalski",
        "jan.kowalski@google.com",
        "pl",
        null,
        "nDVAZXAEt1VvrYrazvxmU8yruiur9cJg",
        24L
    );

    // Act: send registration email.
    userSendEmailService.sendRegistrationEmail(event);

    // Assert that email (account registration) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      // Assert that correct email request was sent.
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jan Kowalski");
      params.put("activationLink", frontendWww+"vue/activate?token=nDVAZXAEt1VvrYrazvxmU8yruiur9cJg");
      params.put("activationTokenExpires", userHelperService.resolveExpirationTime(EnUserTokenType.ACTIVATE));
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "pl",
          emailSender,
          List.of("jan.kowalski@google.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": rejestracja konta",
          "user/registration",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  @Test
  public void sendActivationEmail() {
    // Arrange: event data.
    UserActivatedEvent event = new UserActivatedEvent(
        1L,
        "Jan Kowalski",
        "jan.kowalski@google.com",
        "pl",
        null
    );

    // Act: send 'user activated' email.
    userSendEmailService.sendActivatedEmail(event);

    // Assert that email (confirmation of account activate) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jan Kowalski");
      params.put("loginLink", frontendWww+"vue/login");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "pl",
          emailSender,
          List.of("jan.kowalski@google.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": witamy",
          "user/activation",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  @Test
  public void sendAlreadyRegisteredEmail() {
    // Arrange: event data.
    UserAlreadyRegisteredEvent event = new UserAlreadyRegisteredEvent(
        1L,
        "Jan Kowalski",
        "jan.kowalski@google.com",
        "pl",
        null
    );

    // Act: send 'already registered' email.
    userSendEmailService.sendAlreadyRegisteredEmail(event);

    // Assert that email (confirmation of account activate) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jan Kowalski");
      params.put("loginLink", frontendWww+"vue/login");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "pl",
          emailSender,
          List.of("jan.kowalski@google.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": witamy",
          "user/alreadyRegistered",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  //

  @Test
  public void sendEmailChangeRequest() {
    // Arrange: event data.
    UserEmailChangeRequestEvent event = new UserEmailChangeRequestEvent(
        1L,
        "Jane",
        "test@example.com",
        "en",
        null,
        "other@example.com",
        "nDVAZXAEt1VvrYrazvxmU8yruiur9cJg",
        userHelperService.resolveExpirationTime(EnUserTokenType.EMAIL)
    );

    // Act: send email change request emails. Will send two emails: warning and link.
    userSendEmailService.sendEmailChangeRequest(event);

    // Assert that both emails (warning about email change and link to email change page) were sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(2)).sendEmail(captor.capture());

      // Prepare actual and expected data.
      List<EmailReq> allCapturedEmails = captor.getAllValues();
      EmailReq actualWarningReq = allCapturedEmails.get(0);
      EmailReq actualLinkReq = allCapturedEmails.get(1);

      Map<String, Object> paramsWarning = Maps.newHashMap();
      paramsWarning.put("systemName", systemName);
      paramsWarning.put("username", "Jane");
      EmailReq expectedEmailWarningReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("test@example.com"), // OLD email
          List.of(),
          List.of(),
          emailSender,
          systemName+": email change requested",
          "user/email/warning",
          paramsWarning,
          null
      );

      Map<String, Object> paramsLink = Maps.newHashMap();
      paramsLink.put("systemName", systemName);
      paramsLink.put("username", "Jane");
      paramsLink.put("emailChangeLink", frontendWww+"vue/emailChange?token=nDVAZXAEt1VvrYrazvxmU8yruiur9cJg");
      paramsLink.put("emailChangeTokenExpires", 30L);
      EmailReq expectedEmailLinkReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("other@example.com"), // NEW email
          List.of(),
          List.of(),
          emailSender,
          systemName+": email change requested",
          "user/email/link",
          paramsLink,
          null
      );

      // Assert that correct email requests were sent.
      assertThat(actualWarningReq).isEqualTo(expectedEmailWarningReq);
      assertThat(actualLinkReq).isEqualTo(expectedEmailLinkReq);
    });
  }

  @Test
  public void sendEmailChangeFail() {
    // Arrange: event data.
    UserEmailChangeFailEvent event = new UserEmailChangeFailEvent(
        1L,
        "Jane",
        "test@example.com",
        "en",
        null,
        "other@example.com"
    );

    // Act: send email change request emails. Will send two emails: warning and link.
    userSendEmailService.sendEmailChangeFail(event);

    // Assert that both emails (warning about email change and link to email change page) were sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(2)).sendEmail(captor.capture());

      // Prepare actual and expected data.
      List<EmailReq> allCapturedEmails = captor.getAllValues();
      EmailReq actualWarningOldReq = allCapturedEmails.get(0);
      EmailReq actualWarningNewReq = allCapturedEmails.get(1);

      Map<String, Object> paramsWarning = Maps.newHashMap();
      paramsWarning.put("systemName", systemName);
      paramsWarning.put("username", "Jane");
      EmailReq expectedEmailWarningOldReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("test@example.com"), // OLD email
          List.of(),
          List.of(),
          emailSender,
          systemName+": email change requested",
          "user/email/warning",
          paramsWarning,
          null
      );

      Map<String, Object> paramsWarningNew = Maps.newHashMap();
      paramsWarningNew.put("systemName", systemName);
      paramsWarningNew.put("username", "Jane");
      EmailReq expectedEmailWarningNewReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("other@example.com"), // NEW email
          List.of(),
          List.of(),
          emailSender,
          systemName+": email change requested",
          "user/email/warningNew",
          paramsWarningNew,
          null
      );

      // Assert that correct email requests were sent.
      assertThat(actualWarningOldReq).isEqualTo(expectedEmailWarningOldReq);
      assertThat(actualWarningNewReq).isEqualTo(expectedEmailWarningNewReq);
    });
  }

  @Test
  public void sendEmailChangeConfirmation() {
    // Arrange: event data.
    UserEmailChangeConfirmEvent event = new UserEmailChangeConfirmEvent(
        1L,
        "Jane",
        "test@example.com",
        "en"
    );

    // Act: send password reset confirm email.
    userSendEmailService.sendEmailChangeConfirm(event);

    // Assert that email (password reset confirmation) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jane");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("test@example.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": email changed",
          "user/email/confirm",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  //

  @Test
  public void sendPasswordResetLink() {
    // Arrange: event data.
    UserPasswordResetRequestEvent event = new UserPasswordResetRequestEvent(
        1L,
        "Jane",
        "test@example.com",
        "en",
        null,
        "nDVAZXAEt1VvrYrazvxmU8yruiur9cJg",
        userHelperService.resolveExpirationTime(EnUserTokenType.PASSWORD)
    );

    // Act: send password reset link email.
    userSendEmailService.sendPasswordResetRequest(event);

    // Assert that email (link to password reset page) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jane");
      params.put("passwordResetLink", frontendWww+"vue/passwordReset?token=nDVAZXAEt1VvrYrazvxmU8yruiur9cJg");
      params.put("passResetTokenExpires", 30L);
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("test@example.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": password reset requested",
          "user/password/link",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  @Test
  public void sendPasswordResetConfirmation() {
    // Arrange: event data.
    UserPasswordResetConfirmEvent event = new UserPasswordResetConfirmEvent(
        1L,
        "Jane",
        "test@example.com",
        "en"
    );

    // Act: send password reset confirm email.
    userSendEmailService.sendPasswordResetConfirm(event);

    // Assert that email (password reset confirmation) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jane");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("test@example.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": password changed",
          "user/password/confirm",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  //

  @Test
  public void sendAccountDeleteLink() {
    // Arrange: event data.
    UserAccountDeleteRequestEvent event = new UserAccountDeleteRequestEvent(
        1L,
        "Jane",
        "test@example.com",
        "en",
        null,
        "nDVAZXAEt1VvrYrazvxmU8yruiur9cJg",
        userHelperService.resolveExpirationTime(EnUserTokenType.DELETE)
    );

    // Act: send account deletion link email.
    userSendEmailService.sendAccountDeleteRequest(event);

    // Assert that email (link to account deletion page) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jane");
      params.put("accountDeleteLink", "https://pawel-papierkowski.github.io/frontend-userland-vue/accountDelete?token=nDVAZXAEt1VvrYrazvxmU8yruiur9cJg");
      params.put("accountDeleteTokenExpires", 30L);
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("test@example.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": account deletion requested",
          "user/delete/link",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  @Test
  public void sendAccountDeleteConfirmation() {
    // Arrange: event data.
    UserAccountDeleteConfirmEvent event = new UserAccountDeleteConfirmEvent(
        1L,
        "Jane",
        "test@example.com",
        "en"
    );

    // Act: send account delete confirmation email.
    userSendEmailService.sendAccountDeleteConfirm(event);

    // Assert that email (account deletion confirmation) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("systemName", systemName);
      params.put("username", "Jane");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          emailSender,
          List.of("test@example.com"),
          List.of(),
          List.of(),
          emailSender,
          systemName+": account deleted",
          "user/delete/confirm",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }
}
