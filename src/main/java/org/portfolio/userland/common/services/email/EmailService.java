package org.portfolio.userland.common.services.email;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.email.providers.EmailProviderFactory;
import org.portfolio.userland.common.services.email.providers.IntEmailProvider;
import org.portfolio.userland.features.user.services.UserEmailService;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Email service that handles arbitrary email. See intermediate <code>***EmailService</code> beans (for example
 * <code>UserEmailService</code>) for usage.
 * <p>Note: It should be called asynchronously via event.</p>
 * @see UserEmailService
 */
@Service
@RequiredArgsConstructor
public class EmailService {
  private final EmailProviderFactory emailProviderFactory;
  private final TemplateEngine templateEngine;

  /**
   * Send email based on data in email request.
   * @param emailReq Email request.
   */
  public void sendEmail(EmailReq emailReq) {
    emailReq = processTemplate(emailReq);

    // TODO: in future handle code below as background task with retries and other fancy features (message broker?).

    // Determine correct provider.
    IntEmailProvider emailProvider = emailProviderFactory.getProvider(emailReq.provider());
    // Send email using that provider.
    emailProvider.send(emailReq);
  }

  /**
   * Process template if needed.
   * @param emailReq Email request.
   * @return Modified email request.
   */
  private EmailReq processTemplate(EmailReq emailReq) {
    if (!StringUtils.isEmpty(emailReq.messageHtml())) return emailReq;

    Locale userLocale = Locale.forLanguageTag(emailReq.lang());
    Context context = new Context(userLocale);
    context.setVariables(emailReq.params());
    String messageHtml = templateEngine.process(emailReq.template(), context);
    return emailReq.withMessageHtml(messageHtml);
  }
}
