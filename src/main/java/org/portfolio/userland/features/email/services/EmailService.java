package org.portfolio.userland.features.email.services;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.constants.EnAppBuild;
import org.portfolio.userland.common.constants.GeneralConst;
import org.portfolio.userland.common.exception.SystemMisconfigurationException;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.exceptions.InvalidEmailReqException;
import org.portfolio.userland.features.email.services.providers.EmailProviderFactory;
import org.portfolio.userland.features.email.services.providers.IntEmailProvider;
import org.portfolio.userland.features.user.services.standard.UserSendEmailService;
import org.portfolio.userland.gcp.services.GcpEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Set;

/**
 * Email service that handles arbitrary email. See intermediate <code>XxxEmailService</code> beans (for example
 * <code>UserEmailService</code>) for usage.
 * <p>Note: It should be called asynchronously via event.</p>
 * @see UserSendEmailService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
  private final GcpEmailService gcpEmailService;
  private final EmailProviderFactory emailProviderFactory;
  private final TemplateEngine templateEngine;
  private final Validator validator;

  /** System profile. */
  @Value("${app.main.build}")
  private EnAppBuild build;
  /** If true, use GCP Cloud Task for queueing emails. */
  @Value("${app.gcp.email.task}")
  private Boolean canEmailTask;
  /** Configured system sender address - always overrides whatever caller provided. */
  @Value("${app.email.sender}")
  private String sender;

  /**
   * Queue email to be sent later.
   * <p>Request is validated first, so misuse is caught early (at enqueue time) instead of crashing during actual
   * sending.</p>
   * @param emailReq Email request.
   */
  public void queueEmail(EmailReq emailReq) {
    validate(emailReq);
    log.trace("queueEmail() called. Recipients: '{}', template: {}.", emailReq.getRecipients(), emailReq.template());

    emailReq = process(emailReq);

    // GCP Tasks ensure that emails won't be lost in case of failure.
    if (canEmailTask) gcpEmailService.queueEmailTask(emailReq);
    else sendEmail(emailReq); // On locally run server just send synchronically.
  }

  /**
   * Validates given request against constraints declared on {@link EmailReq}. Note the REST boundary
   * (<code>GcpController</code>) validates via <code>@Valid</code> too - this covers internal callers that bypass it.
   * @param emailReq Email request.
   */
  private void validate(EmailReq emailReq) {
    Set<ConstraintViolation<EmailReq>> violations = validator.validate(emailReq);
    if (!violations.isEmpty()) throw new InvalidEmailReqException(violations);
  }

  /**
   * Actually send email based on data in email request.
   * <p>Security note: sender address is always overridden with configured system sender, so no caller (internal
   * code, queued task payload, or anything else) can spoof the 'from' address.</p>
   * @param emailReq Email request.
   */
  public void sendEmail(EmailReq emailReq) {
    log.trace("sendEmail() called. Recipients: '{}', template: {}.", emailReq.getRecipients(), emailReq.template());

    // Force configured sender, ignoring whatever was provided in the request.
    if (StringUtils.isEmpty(sender)) throw new SystemMisconfigurationException("System sender address is not configured!");
    emailReq = emailReq.toBuilder().sender(sender).build();

    // Determine correct provider.
    IntEmailProvider emailProvider = emailProviderFactory.getProvider(emailReq.provider());
    // Send email using that provider.
    emailProvider.send(emailReq);
  }

  //

  /**
   * Process email request. Determines subject and generates HTML of email based on template.
   * Note: stuff like attachments, pictures etc. should be done in sendEmail(), as GCP Task can have at most 100KB.
   * @param emailReq Email request.
   * @return Modified email request.
   */
  private EmailReq process(EmailReq emailReq) {
    String subject = resolveSubject(emailReq);
    String messageHtml = resolveTemplate(emailReq);
    return emailReq.toBuilder()
        .subject(subject)
        .messageHtml(messageHtml)
        .build();
  }

  /**
   * Resolve subject. May be modified.
   * @param emailReq Email request.
   * @return Modified subject.
   */
  private String resolveSubject(EmailReq emailReq) {
    if (!build.getTest()) return emailReq.subject();
    return GeneralConst.TEST_INDICATOR + " " + emailReq.subject();
  }

  /**
   * Resolve template if needed.
   * @param emailReq Email request.
   * @return HTML message.
   */
  private String resolveTemplate(EmailReq emailReq) {
    if (!StringUtils.isEmpty(emailReq.messageHtml())) return emailReq.messageHtml();

    // Null-safe language fallback.
    Locale userLocale = Locale.forLanguageTag(StringUtils.defaultIfEmpty(emailReq.lang(), "en"));
    Context context = new Context(userLocale);
    context.setVariables(emailReq.params());
    return templateEngine.process(emailReq.template(), context);
  }
}
