package org.portfolio.userland.common.services.email.providers;

import org.portfolio.userland.common.services.email.data.EmailReq;

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
