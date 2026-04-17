package org.portfolio.userland.features.user.services;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.lang.LangService;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetConfirmEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetSendEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

/**
 * Handles emails that are related to user. Note: uses separate async thread.
 * <p>Currently handles:</p>
 * <ul>
 *   <li>User registration (sends email with activation link)</li>
 *   <li>User activation (sends email confirming successful activation of user account)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserEmailService {
  private final static String TEMPLATE_USER_REGISTRATION = "user/registration";
  private final static String TEMPLATE_USER_ACTIVATION = "user/activation";
  private final static String TEMPLATE_USER_PASSWORD_RESET = "user/passwordReset";
  private final static String TEMPLATE_USER_PASSWORD_CONFIRM = "user/passwordConfirm";

  private final EmailService emailService;
  private final LangService langService;

  /** Frontend address. */
  @Value("${app.main.www}")
  private String frontendWww;
  /** Who sends emails? */
  @Value("${app.email.sender}")
  private String emailSender;

  /**
   * React on user registration event.
   * Note: @TransactionalEventListener annotation ensures event publisher finish its work first.
   * So it is safe to get user entity here - it is guaranteed to exist in database.
   * Downside is that you cannot use @Transactional here, so event has to carry all needed data
   * without querying database.
   * @param event User registration event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendRegistrationEmail(UserRegisteredEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for registration.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserRegisteredEvent event) {
    String subject = langService.t(event.lang(), "email.user.registration.subject");

    // Prepare params required by user registration template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("activationLink", resolveActivationLink(event.activationToken()));
    params.put("activationTokenExpires", event.activationTokenExpires());

    return new EmailReq(
        null, // use default provider
        event.lang(),
        emailSender,
        List.of(event.email()), // where email will be sent
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_REGISTRATION,
        params,
        null);
  }

  /**
   * Resolve full activation link. Note it is for frontend, not backend.
   * @param activationToken Activation token.
   * @return Activation link.
   */
  private String resolveActivationLink(String activationToken) {
    // Note it is link to frontend - actual backend activation endpoint will be called by frontend.
    return frontendWww + "/activate?token="+activationToken;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * React on user activation event. Will send email confirming successful activation of user account.
   * @param event User activation event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendActivationEmail(UserActivatedEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for activation.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserActivatedEvent event) {
    String subject = langService.t(event.lang(), "email.user.activation.subject");

    // Prepare params required by user activation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null, // use default provider
        event.lang(),
        emailSender,
        List.of(event.email()), // where email will be sent
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_ACTIVATION,
        params,
        null);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * React on password reset send event. Will send email with link that leads to page where you can change password.
   * @param event Password reset send event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPasswordResetLink(UserPasswordResetSendEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for sending password reset link.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserPasswordResetSendEvent event) {
    String subject = langService.t(event.lang(), "email.user.password.reset.subject");

    // Prepare params required by user activation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("passwordResetLink", resolvePasswordResetLink(event.passwordResetToken()));
    params.put("passResetTokenExpires", event.passwordResetTokenExpires());

    return new EmailReq(
        null, // use default provider
        event.lang(),
        emailSender,
        List.of(event.email()), // where email will be sent
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_PASSWORD_RESET,
        params,
        null);
  }

  /**
   * Resolve full password reset link. Note it is for frontend, not backend.
   * @param passwordResetToken Password reset token.
   * @return Password reset link.
   */
  private String resolvePasswordResetLink(String passwordResetToken) {
    // Note it is link to frontend - actual backend password reset endpoint will be called by frontend.
    return frontendWww + "/passwordReset?token="+passwordResetToken;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * React on password reset confirmation event. Will send email with link that leads to page where you can change password.
   * @param event Password reset send event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPasswordResetConfirmation(UserPasswordResetConfirmEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for password reset.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserPasswordResetConfirmEvent event) {
    String subject = langService.t(event.lang(), "email.user.password.confirm.subject");

    // Prepare params required by user activation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null, // use default provider
        event.lang(),
        emailSender,
        List.of(event.email()), // where email will be sent
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_PASSWORD_CONFIRM,
        params,
        null);
  }
}
