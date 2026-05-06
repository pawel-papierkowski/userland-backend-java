package org.portfolio.userland.features.email.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.constants.EnAppBuild;
import org.portfolio.userland.common.constants.GeneralConst;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.providers.EmailProviderFactory;
import org.portfolio.userland.features.email.services.providers.IntEmailProvider;
import org.portfolio.userland.features.user.services.UserSendEmailService;
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
public class EmailService {
  private final EmailProviderFactory emailProviderFactory;
  private final TemplateEngine templateEngine;

  /** System profile. */
  @Value("${app.main.build}")
  private EnAppBuild build;

  /**
   * Send email based on data in email request.
   * @param emailReq Email request.
   */
  public void sendEmail(EmailReq emailReq) {
    emailReq = process(emailReq);

    // TODO: in future handle code below as background task with retries and other fancy features (message broker?).

    // Determine correct provider.
    IntEmailProvider emailProvider = emailProviderFactory.getProvider(emailReq.provider());
    // Send email using that provider.
    emailProvider.send(emailReq);
  }

  /**
   * Process email request.
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
