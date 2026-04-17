package org.portfolio.userland.features.user;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetConfirmEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetSendEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.portfolio.userland.features.user.services.UserEmailService;
import org.springframework.beans.factory.annotation.Autowired;

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
public class UserEmailTest extends BaseUserTest {
  @Autowired
  private UserEmailService userEmailService;

  @Test
  public void sendRegistrationEmail() {
    // Arrange: event data.
    UserRegisteredEvent event = new UserRegisteredEvent(
        1L,
        "Jan Kowalski",
        "jan.kowalski@google.com",
        "pl",
        "nDVAZXAEt1VvrYrazvxmU8yruiur9cJg",
        24L
    );

    // Act: send registration email.
    userEmailService.sendRegistrationEmail(event);

    // Assert that email (account registration) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      // Assert that correct email request was sent.
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      Map<String, Object> params = Maps.newHashMap();
      params.put("username", "Jan Kowalski");
      params.put("activationLink", "https://pawel-papierkowski.github.io/frontend-userland-vue/activate?token=nDVAZXAEt1VvrYrazvxmU8yruiur9cJg");
      params.put("activationTokenExpires", 24L);
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "pl",
          "pawel.papierkowski.portfolio@gmail.com",
          List.of("jan.kowalski@google.com"),
          List.of(),
          List.of(),
          "pawel.papierkowski.portfolio@gmail.com",
          "UserLand: rejestracja konta",
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
        "pl"
    );

    // Act: send registration email.
    userEmailService.sendActivationEmail(event);

    // Assert that email (confirmation of account activation) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("username", "Jan Kowalski");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "pl",
          "pawel.papierkowski.portfolio@gmail.com",
          List.of("jan.kowalski@google.com"),
          List.of(),
          List.of(),
          "pawel.papierkowski.portfolio@gmail.com",
          "UserLand: witamy",
          "user/activation",
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
    UserPasswordResetSendEvent event = new UserPasswordResetSendEvent(
        1L,
        "Jane",
        "test@example.com",
        "en",
        "nDVAZXAEt1VvrYrazvxmU8yruiur9cJg",
        30
    );

    // Act: send registration email.
    userEmailService.sendPasswordResetLink(event);

    // Assert that email (link to password reset page) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("username", "Jane");
      params.put("passwordResetLink", "https://pawel-papierkowski.github.io/frontend-userland-vue/passwordReset?token=nDVAZXAEt1VvrYrazvxmU8yruiur9cJg");
      params.put("passResetTokenExpires", 30L);
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          "pawel.papierkowski.portfolio@gmail.com",
          List.of("test@example.com"),
          List.of(),
          List.of(),
          "pawel.papierkowski.portfolio@gmail.com",
          "UserLand: password reset",
          "user/passwordReset",
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

    // Act: send registration email.
    userEmailService.sendPasswordResetConfirmation(event);

    // Assert that email (password reset confirmation) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("username", "Jane");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          "pawel.papierkowski.portfolio@gmail.com",
          List.of("test@example.com"),
          List.of(),
          List.of(),
          "pawel.papierkowski.portfolio@gmail.com",
          "UserLand: password changed",
          "user/passwordConfirm",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }
}
