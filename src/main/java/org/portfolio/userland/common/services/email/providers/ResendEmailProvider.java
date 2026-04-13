package org.portfolio.userland.common.services.email.providers;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.email.exception.EmailSendFailureException;
import org.springframework.stereotype.Service;

/**
 * Handles Transactional Email Provider called Resend.
 * TODO: preliminary code, will update it
 */
@Service
@RequiredArgsConstructor
public class ResendEmailProvider implements IntEmailProvider {
  /** Resend SDK. We created bean for it in EmailConfig. */
  private final Resend resend;

  @Override
  public String getProviderName() {
    return "resend";
  }

  @Override
  public void send(EmailReq emailReq) {
    CreateEmailOptions params = CreateEmailOptions.builder()
        .from(emailReq.sender()) // Acme <onboarding@resend.dev>
        .replyTo(emailReq.replyTo())
        .to(emailReq.recipients()) // delivered@resend.dev
        .subject(emailReq.subject())
        .html(emailReq.messageHtml()) // "<strong>hello world</strong>"
        .build();

    try {
      resend.emails().send(params);
    } catch (ResendException ex) {
      throw new EmailSendFailureException("Resend", ex);
    }
  }
}
