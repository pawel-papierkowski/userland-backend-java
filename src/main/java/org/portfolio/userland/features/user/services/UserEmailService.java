package org.portfolio.userland.features.user.services;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.lang.LangService;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.portfolio.userland.features.user.events.*;
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
 *   <li>Password reset link</li>
 *   <li>Password reset confirmation</li>
 *   <li>Account deletion link</li>
 *   <li>Account deletion confirmation</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserEmailService {
  private final static String FRONTEND_DEFAULT = EnFrontendFramework.VUE.name().toLowerCase();

  private final static String TEMPLATE_USER_REGISTRATION = "user/registration";
  private final static String TEMPLATE_USER_ACTIVATION = "user/activation";
  private final static String TEMPLATE_USER_PASSWORD_LINK = "user/password/link";
  private final static String TEMPLATE_USER_PASSWORD_CONFIRM = "user/password/confirm";
  private final static String TEMPLATE_USER_DELETE_LINK = "user/delete/link";
  private final static String TEMPLATE_USER_DELETE_CONFIRM = "user/delete/confirm";

  private final EmailService emailService;
  private final LangService langService;

  /** Base frontend address. */
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
    params.put("activationLink", resolveActivationLink(event.frontend(), event.activationToken()));
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
   * @param frontend Name of used frontend.
   * @param activationToken Activation token.
   * @return Activation link.
   */
  private String resolveActivationLink(EnFrontendFramework frontend, String activationToken) {
    // Note it is linking to frontend - actual backend activation endpoint will be called by frontend.
    return resolveWww(frontend) + "/activate?token="+activationToken;
  }

  //

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
    params.put("loginLink", resolveLoginLink(event.frontend()));

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

  /**
   * Resolve login link. Note it is for frontend, not backend.
   * @param frontend Name of used frontend.
   * @return Login link.
   */
  private String resolveLoginLink(EnFrontendFramework frontend) {
    // Note it is linking to frontend - actual backend login endpoint will be called by frontend.
    return resolveWww(frontend) + "/login";
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * React on password reset link event. Will send email with link that leads to page where you can change password.
   * @param event Password reset link event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPasswordResetLink(UserPasswordResetLinkEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for sending password reset link.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserPasswordResetLinkEvent event) {
    String subject = langService.t(event.lang(), "email.user.password.link.subject");

    // Prepare params required by user activation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("passwordResetLink", resolvePasswordResetLink(event.frontend(), event.passwordResetToken()));
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
        TEMPLATE_USER_PASSWORD_LINK,
        params,
        null);
  }

  /**
   * Resolve full password reset link. Note it is for frontend, not backend.
   * @param frontend Name of used frontend.
   * @param passwordResetToken Password reset token.
   * @return Password reset link.
   */
  private String resolvePasswordResetLink(EnFrontendFramework frontend, String passwordResetToken) {
    // Note it is linking to frontend - actual backend password reset endpoint will be called by frontend.
    return resolveWww(frontend) + "/passwordReset?token="+passwordResetToken;
  }

  //

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

  // //////////////////////////////////////////////////////////////////////////

  /**
   * React on account delete link event. Will send email with link that leads to page where you can delete your account.
   * @param event Account delete link event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendAccountDeleteLink(UserAccountDeleteLinkEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for sending account delete link.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserAccountDeleteLinkEvent event) {
    String subject = langService.t(event.lang(), "email.user.delete.link.subject");

    // Prepare params required by user activation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("accountDeleteLink", resolveAccountDeleteLink(event.frontend(), event.accountDeleteToken()));
    params.put("accountDeleteTokenExpires", event.accountDeleteTokenExpires());

    return new EmailReq(
        null, // use default provider
        event.lang(),
        emailSender,
        List.of(event.email()), // where email will be sent
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_DELETE_LINK,
        params,
        null);
  }

  /**
   * Resolve full account delete link. Note it is for frontend, not backend.
   * @param frontend Name of used frontend.
   * @param accountDeleteToken Account delete token.
   * @return Account delete link.
   */
  private String resolveAccountDeleteLink(EnFrontendFramework frontend, String accountDeleteToken) {
    // Note it is linking to frontend - actual backend account delete endpoint will be called by frontend.
    return resolveWww(frontend) + "/accountDelete?token="+accountDeleteToken;
  }

  //

  /**
   * React on account delete confirmation event. Will send email with account delete confirmation.
   * @param event Account delete link event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendAccountDeleteConfirmation(UserAccountDeleteConfirmEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for account delete.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserAccountDeleteConfirmEvent event) {
    String subject = langService.t(event.lang(), "email.user.delete.confirm.subject");

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
        TEMPLATE_USER_DELETE_CONFIRM,
        params,
        null);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Resolve WWW address of frontend. It consists of base www address (frontendWww) and suffix indicating what frontend
   * framework was used.
   * @param frontend Used frontend framework.
   * @return WWW address of frontend. Example: https://pawel-papierkowski.github.io/frontend-userland-vue
   */
  private String resolveWww(EnFrontendFramework frontend) {
    String suffix = frontend == null ? FRONTEND_DEFAULT : frontend.name().toLowerCase();
    return frontendWww + suffix;
  }
}
