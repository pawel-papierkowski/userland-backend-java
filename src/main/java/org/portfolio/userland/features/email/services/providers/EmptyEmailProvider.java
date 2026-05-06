package org.portfolio.userland.features.email.services.providers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.springframework.stereotype.Service;

/**
 * This email provider does nothing. Useful for mocking purposes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmptyEmailProvider implements IntEmailProvider {
  @Override
  public String getProviderName() {
    return "empty";
  }

  @Override
  public void send(EmailReq emailReq) {
    // NOOP
  }
}
