package org.portfolio.userland.common.services.security;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Generates needed secure hashes, tokens, UUIDs etc.
 */
@Service
@RequiredArgsConstructor
public class SecurityGeneratorService {
  /** Length of token. */
  @Value("${app.user.token.length}")
  private int tokenLength;

  /**
   * Generate token. It is alphanumeric string of length configured in yaml: user.token.length.
   * @return Token.
   */
  public String token() {
    return RandomStringUtils.secure().nextAlphanumeric(tokenLength);
  }

  /**
   * Generate type 4 UUID.
   * @return UUID.
   */
  public UUID uuid() {
    return UUID.randomUUID();
  }
}
