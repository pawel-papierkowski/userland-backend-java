package org.portfolio.userland.common.services.email;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.common.services.email.providers.EmailProviderFactory;
import org.portfolio.userland.common.services.email.providers.IntEmailProvider;
import org.springframework.stereotype.Service;

/**
 * Email service. TODO: right now, this is placeholder.
 */
@Service
@RequiredArgsConstructor
public class EmailService {
  private final EmailProviderFactory emailProviderFactory;

  /**
   * Send email based on data in email request.
   * @param emailReq Email request.
   */
  public void sendEmail(EmailReq emailReq) {
    // TODO: Use templating engine to process data.
    // Determine correct provider.
    IntEmailProvider emailProvider = emailProviderFactory.getProvider(emailReq.provider());
    // Send email using that provider.
    emailProvider.send(emailReq);
  }
}
