package org.portfolio.userland.features.email.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.constants.EnAppBuild;
import org.portfolio.userland.common.constants.GeneralConst;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.providers.EmailProviderFactory;
import org.portfolio.userland.features.email.services.providers.IntEmailProvider;
import org.portfolio.userland.features.user.services.UserSendEmailService;
import org.portfolio.userland.gcp.services.GcpEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Email service that handles arbitrary email. See intermediate <code>***EmailService</code> beans (for example
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

  /** System profile. */
  @Value("${app.main.build}")
  private EnAppBuild build;
  /** If true, use GCP Cloud Task for queueing emails. */
  @Value("${app.gcp.email.task}")
  private Boolean canEmailTask;

  /**
   * Queue email to be sent later.
   * @param emailReq Email request.
   */
  public void queueEmail(EmailReq emailReq) {
    emailReq = process(emailReq);

    // GCP Tasks ensure that emails won't be lost in case of failure.
    if (canEmailTask) gcpEmailService.queueEmailTask(emailReq);
    else sendEmail(emailReq); // just send synchronically
  }

  /**
   * Actually send email based on data in email request.
   * @param emailReq Email request.
   */
  public void sendEmail(EmailReq emailReq) {
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
   * Resolve subject.
   * @param emailReq Email request.
   * @return New version of subject.
   */
  private String resolveSubject(EmailReq emailReq) {
    if (!build.getTest()) return emailReq.subject();
    return GeneralConst.TEST_INDICTATOR + " " + emailReq.subject();
  }

  /**
   * Resolve template if needed.
   * @param emailReq Email request.
   * @return HTML message.
   */
  private String resolveTemplate(EmailReq emailReq) {
    if (!StringUtils.isEmpty(emailReq.messageHtml())) return emailReq.messageHtml();

    Locale userLocale = Locale.forLanguageTag(emailReq.lang());
    Context context = new Context(userLocale);
    context.setVariables(emailReq.params());
    return templateEngine.process(emailReq.template(), context);
  }
}
