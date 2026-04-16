package org.portfolio.userland.features.user.services;

import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.data.*;
import org.portfolio.userland.features.user.exception.UserTokenExpiredException;
import org.portfolio.userland.features.user.exception.UserTokenMissingException;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

/**
 * Base for all user services.
 */
public abstract class BaseUserService {
  @Autowired
  private SecurityGeneratorService securityGeneratorService;
  @Autowired
  private UserHelperService userHelperService;

  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserTokenRepository userTokenRepository;
  @Autowired
  protected ApplicationEventPublisher eventPublisher;

  @Autowired
  protected ClockService clockService;

  //

  /**
   * Create and fill token data.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return User token entry.
   */
  protected UserToken createTokenData(LocalDateTime nowAt, EnTokenType type) {
    UserToken token = new UserToken();
    token.setCreatedAt(nowAt);
    token.setExpiresAt(userHelperService.resolveExpiration(nowAt, type));
    token.setType(type);
    token.setToken(securityGeneratorService.token());
    return token;
  }

  /**
   * Create and fill history event.
   * @param nowAt Current date&time.
   * @param what What happened.
   * @return User history event.
   */
  protected UserHistory createHistoryEvent(LocalDateTime nowAt, EnHistoryWhat what) {
    UserHistory event = new UserHistory();
    event.setUuid(securityGeneratorService.uuid());
    event.setCreatedAt(nowAt);
    event.setWho(EnHistoryWho.USER);
    event.setWhat(what);
    return event;
  }

  //

  /**
   * Retrieve user token based on token string. Will throw exception if token is not found or is expired.
   * @param tokenStr Token string.
   */
  protected UserToken resolveToken(LocalDateTime nowAt, EnTokenType type, String tokenStr) {
    UserToken userToken = userTokenRepository.findByTypeAndToken(type, tokenStr)
        .orElseThrow(() -> new UserTokenMissingException(tokenStr));
    if (userToken.getExpiresAt().isBefore(nowAt)) throw new UserTokenExpiredException(tokenStr);
    return userToken;
  }
}
