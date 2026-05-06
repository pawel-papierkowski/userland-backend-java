package org.portfolio.userland.features.email.services.providers;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.exceptions.EmailSendFailureException;
import org.springframework.stereotype.Service;

/**
 * Handles Transactional Email Provider called Resend.
 * <p>Notes:</p>
 * <ul>
 *   <li>Resend provides special email address that you can use for testing: <b>delivered@resend.dev</b></li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailProvider implements IntEmailProvider {
  /** Resend SDK. We created bean for it in EmailConfig. */
  private final Resend resend;

  @Override
  public String getProviderName() {
    return "resend";
  }

  @Override
  public void send(EmailReq emailReq) {
    try {
      CreateEmailOptions params = CreateEmailOptions.builder()
        .from(emailReq.sender())
        .replyTo(emailReq.replyTo())
        .to(emailReq.recipients()) // delivered@resend.dev
        .subject(emailReq.subject())
        .html(emailReq.messageHtml())
        .build();
      resend.emails().send(params);

      log.trace("Sent email to '{}'. Template: '{}', lang: '{}'.", emailReq.recipients().getFirst(), emailReq.template(), emailReq.lang());
    } catch (ResendException ex) {
      throw new EmailSendFailureException("Resend", ex);
    }
  }
}
