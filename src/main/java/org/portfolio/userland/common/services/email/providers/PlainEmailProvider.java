package org.portfolio.userland.common.services.email.providers;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.email.exception.EmailSendFailureException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends email via standard email service (SMTP) using JavaMailSender.
 */
@Service
@RequiredArgsConstructor
public class PlainEmailProvider implements IntEmailProvider {
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // IDE is dumb
  private final JavaMailSender mailSender;

  @Override
  public String getProviderName() {
    return "plain";
  }

  @Override
  public void send(EmailReq emailReq) {
    try {
      // Ask Spring to create the empty MimeMessage.
      MimeMessage message = mailSender.createMimeMessage();

      // Wrap it in a MimeMessageHelper.
      // The 'true' flag indicates that this is a multipart message (needed for attachments or inline images).
      // 'UTF-8' ensures special characters render correctly.
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      // Set the email details.
      helper.setFrom(emailReq.sender());
      helper.setTo(emailReq.recipients().toArray(new String[]{}));
      helper.setSubject(emailReq.subject());

      // Set the body. The 'true' flag tells Spring this string contains HTML.
      helper.setText(emailReq.messageHtml(), true);

      // Send it.
      mailSender.send(message);
    } catch (jakarta.mail.MessagingException ex) {
      // MimeMessageHelper throws checked exceptions, so we must handle them.
      throw new EmailSendFailureException("JavaMailSender", ex);
    }
  }
}
