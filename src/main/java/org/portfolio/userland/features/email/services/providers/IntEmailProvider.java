package org.portfolio.userland.features.email.services.providers;

import org.portfolio.userland.features.email.dto.EmailReq;

/**
 * Interface common for all email providers.
 */
public interface IntEmailProvider {
  /**
   * Name of provider.
   * @return Name of provider.
   */
  String getProviderName();

  /**
   * Send email using this provider.
   * @param emailReq Email request.
   */
  void send(EmailReq emailReq);
}
