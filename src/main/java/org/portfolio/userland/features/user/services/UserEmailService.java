package org.portfolio.userland.features.user.services;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Handles emails that are related to user.
 */
@Service
@RequiredArgsConstructor
public class UserEmailService {
  private final static String TEMPLATE_USER_ACTIVATION = "user/activation";

  private final EmailService emailService;
  private final UserRepository userRepository;

  /** Frontend address. */
  @Value("${app.main.www}")
  private String frontendWww;
  /** Who sends emails? */
  @Value("${app.email.sender}")
  private String emailSender;
  /** How long before activation token expires in hours. */
  @Value("${app.user.token.activation.expires}")
  private long activationTokenExpires;

  /**
   * React on user registration event.
   * Note: @TransactionalEventListener annotation ensures event publisher finish its work first.
   * So it is safe to get user entity here - it is guaranteed to exist in database.
   * @param event User registration event data.
   */
  @Async("emailTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendActivationEmail(UserRegisteredEvent event) {
    // Prepare params required by user activation template.
    Map<String, Object> params = Maps.newHashMap();
    params.put("username", event.username());
    params.put("activationLink", resolveActivationLink(event.activationToken()));
    params.put("linkValidXhours", activationTokenExpires);

    // Prepare email request itself.
    EmailReq emailReq = new EmailReq(
        null, // use default provider
        event.lang(),
        emailSender,
        new String[] { event.email() }, // where email will be sent
        new String[] {},
        new String[] {},
        emailSender,
        "SUBJECT", // No subject needed for template. TODO verify it works correctly
        TEMPLATE_USER_ACTIVATION,
        params,
        null); // template engine will create HTML message

    // Finally send email...
    emailService.sendEmail(emailReq);
  }

  private String resolveActivationLink(String activationToken) {
    // Note it is link to frontend - actual backend activation link will be called by frontend.
    return frontendWww + "/activate?token="+activationToken;
  }
}
