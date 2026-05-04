package org.portfolio.userland.features.user.services;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.lang.LangService;
import org.portfolio.userland.features.user.constants.UserConst;
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
 * Handles sending emails that are related to user. Note: uses separate async thread.
 * <p>Currently handles emails for:</p>
 * <ul>
 *   <li>User registration (sends email with activation link)</li>
 *   <li>User activation (sends email confirming successful activation of user account)</li>
 *   <li>User already registered (sends email informing about registration try)</li>
 *   <li>Email change warning (old account)</li>
 *   <li>Email change link (new account)</li>
 *   <li>Email change confirmation (new account)</li>
 *   <li>Password reset link</li>
 *   <li>Password reset confirmation</li>
 *   <li>Account deletion link</li>
 *   <li>Account deletion confirmation</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserSendEmailService {
  private final static String TEMPLATE_USER_REGISTRATION = "user/registration";
  private final static String TEMPLATE_USER_ACTIVATION = "user/activation";
  private final static String TEMPLATE_USER_ALREADY_REGISTERED = "user/alreadyRegistered";

  private final static String TEMPLATE_USER_EMAIL_WARNING = "user/email/warning"; // to old (current) address
  private final static String TEMPLATE_USER_EMAIL_WARNING_NEW = "user/email/warningNew"; // to new address
  private final static String TEMPLATE_USER_EMAIL_LINK = "user/email/link"; // to new address
  private final static String TEMPLATE_USER_EMAIL_CONFIRM = "user/email/confirm"; // to new (and now current) address

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
    // Note it is linking to frontend - actual backend endpoint for user activation will be called by frontend.
    return resolveWww(frontend) + "/activate?token="+activationToken;
  }

  //

  /**
   * React on user activation event. Will send email confirming successful activation of user account.
   * @param event User activation event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendActivatedEmail(UserActivatedEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for activated user.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserActivatedEvent event) {
    String subject = langService.t(event.lang(), "email.user.activation.subject");

    // Prepare params required by user activated template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("loginLink", resolveLoginLink(event.frontend()));

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()),
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_ACTIVATION,
        params,
        null);
  }

  //

  /**
   * React on user already registered event. Will send email warning that someone tried to register user account.
   * @param event User activation event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendAlreadyRegisteredEmail(UserAlreadyRegisteredEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for activated user.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserAlreadyRegisteredEvent event) {
    String subject = langService.t(event.lang(), "email.user.alreadyRegistered.subject");

    // Prepare params required by user activated template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("loginLink", resolveLoginLink(event.frontend()));

    return new EmailReq(
        null, // use default provider
        event.lang(),
        emailSender,
        List.of(event.email()),
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_ALREADY_REGISTERED,
        params,
        null);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * React on email change request event. Will send two emails:
   * <ul>
   *   <li>first to OLD email account with warning about change of email</li>
   *   <li>second to NEW email account with link that leads to page where you can change email</li>
   * </ul>
   *
   * @param event Email change request event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendEmailChangeRequest(UserEmailChangeRequestEvent event) {
    EmailReq emailWarnReq = resolveEmailWarnReq(event);
    emailService.sendEmail(emailWarnReq);
    EmailReq emailLinkReq = resolveEmailLinkReq(event);
    emailService.sendEmail(emailLinkReq);
  }

  /**
   * Prepare email for sending email change warning to OLD email.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailWarnReq(UserEmailChangeRequestEvent event) {
    String subject = langService.t(event.lang(), "email.user.email.warning.subject");

    // Prepare params required by email change warning template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()), // OLD email account
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_EMAIL_WARNING,
        params,
        null);
  }

  /**
   * Prepare email for sending email change link.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailLinkReq(UserEmailChangeRequestEvent event) {
    String subject = langService.t(event.lang(), "email.user.email.link.subject");

    // Prepare params required by email change link template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("emailChangeLink", resolveEmailChangeLink(event.frontend(), event.emailChangeToken()));
    params.put("emailChangeTokenExpires", event.emailChangeTokenExpires());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.newEmail()), // NEW email account
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_EMAIL_LINK,
        params,
        null);
  }

  /**
   * Resolve full email change link. Note it is for frontend, not backend.
   * @param frontend Name of used frontend.
   * @param emailChangeToken Email change token.
   * @return Email change link.
   */
  private String resolveEmailChangeLink(EnFrontendFramework frontend, String emailChangeToken) {
    // Note it is linking to frontend - actual backend email change endpoint will be called by frontend.
    return resolveWww(frontend) + "/emailChange?token="+emailChangeToken;
  }

  //

  /**
   * React on email change fail event. Will send two emails:
   * <ul>
   *   <li>first to OLD email account with warning about change of email</li>
   *   <li>second to NEW email account with warning about change of email</li>
   * </ul>
   *
   * @param event Email change request event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendEmailChangeFail(UserEmailChangeFailEvent event) {
    EmailReq emailWarnOldReq = resolveEmailWarnOldReq(event);
    emailService.sendEmail(emailWarnOldReq);
    EmailReq emailWarnNewReq = resolveEmailWarnNewReq(event);
    emailService.sendEmail(emailWarnNewReq);
  }

  /**
   * Prepare email for sending email change warning to OLD email.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailWarnOldReq(UserEmailChangeFailEvent event) {
    String subject = langService.t(event.lang(), "email.user.email.warning.subject");

    // Prepare params required by email change warning template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()), // OLD email account
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_EMAIL_WARNING,
        params,
        null);
  }

  /**
   * Prepare email for sending email change warning to NEW email.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailWarnNewReq(UserEmailChangeFailEvent event) {
    String subject = langService.t(event.lang(), "email.user.email.warningNew.subject");

    // Prepare params required by email change warning template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.newEmail()), // NEW email account
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_EMAIL_WARNING_NEW,
        params,
        null);
  }

  //

  /**
   * React on email change confirm event. Will send email confirming that you changed email successfully.
   * @param event Email change confirm event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendEmailChangeConfirm(UserEmailChangeConfirmEvent event) {
    EmailReq emailLinkReq = resolveEmailChangeConfirmReq(event);
    emailService.sendEmail(emailLinkReq);
  }

  /**
   * Prepare email for sending email change warning.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailChangeConfirmReq(UserEmailChangeConfirmEvent event) {
    String subject = langService.t(event.lang(), "email.user.email.confirm.subject");

    // Prepare params required by email change warning template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()), // current (NEW) email account
        List.of(),
        List.of(),
        emailSender,
        subject,
        TEMPLATE_USER_EMAIL_CONFIRM,
        params,
        null);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * React on password reset request event. Will send email with link that leads to page where you can change password.
   * @param event Password reset request event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPasswordResetRequest(UserPasswordResetRequestEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for sending password reset link.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserPasswordResetRequestEvent event) {
    String subject = langService.t(event.lang(), "email.user.password.link.subject");

    // Prepare params required by password reset template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("passwordResetLink", resolvePasswordResetLink(event.frontend(), event.passwordResetToken()));
    params.put("passResetTokenExpires", event.passwordResetTokenExpires());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()),
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
   * React on password reset confirm event. Will send email confirming that you changed password successfully.
   * @param event Password reset confirm event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPasswordResetConfirm(UserPasswordResetConfirmEvent event) {
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

    // Prepare params required by password reset confirmation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()),
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
   * React on account delete request event. Will send email with link that leads to page where you can delete your account.
   * @param event Account delete request event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendAccountDeleteRequest(UserAccountDeleteRequestEvent event) {
    EmailReq emailReq = resolveEmailReq(event);
    emailService.sendEmail(emailReq);
  }

  /**
   * Prepare email request for sending account delete link.
   * @param event Event.
   * @return Email request.
   */
  private EmailReq resolveEmailReq(UserAccountDeleteRequestEvent event) {
    String subject = langService.t(event.lang(), "email.user.delete.link.subject");

    // Prepare params required by user account deletion template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("accountDeleteLink", resolveAccountDeleteLink(event.frontend(), event.accountDeleteToken()));
    params.put("accountDeleteTokenExpires", event.accountDeleteTokenExpires());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()),
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
   * React on account delete confirm event. Will send email with confirmation of account deletion.
   * @param event Account delete confirm event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendAccountDeleteConfirm(UserAccountDeleteConfirmEvent event) {
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

    // Prepare params required by user account delete confirmation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());

    return new EmailReq(
        null,
        event.lang(),
        emailSender,
        List.of(event.email()),
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
   * Resolve login link. Note it is for frontend, not backend.
   * @param frontend Name of used frontend.
   * @return Login link.
   */
  private String resolveLoginLink(EnFrontendFramework frontend) {
    // Note it is linking to frontend - actual backend login endpoint will be called by frontend.
    return resolveWww(frontend) + "/login";
  }

  /**
   * Resolve WWW address of frontend. It consists of base www address (frontendWww) and suffix indicating what frontend
   * framework was used.
   * @param frontend Used frontend framework.
   * @return WWW address of frontend. Example: https://pawel-papierkowski.github.io/frontend-userland-vue
   */
  private String resolveWww(EnFrontendFramework frontend) {
    String suffix = frontend == null ? UserConst.FRONTEND_DEF.name().toLowerCase() : frontend.name().toLowerCase();
    return frontendWww + suffix;
  }
}
