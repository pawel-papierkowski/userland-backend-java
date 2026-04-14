package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.data.*;
import org.portfolio.userland.features.user.dto.activate.TokenActivateReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.exception.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.exception.UserTokenExpiredException;
import org.portfolio.userland.features.user.exception.UserTokenMissingException;
import org.portfolio.userland.features.user.mapper.UserRegisterMapper;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for user registration.
 */
@Service
@RequiredArgsConstructor
public class UserRegisterService {
  private final SecurityGeneratorService securityGeneratorService;

  private final UserRepository userRepository;
  private final UserTokenRepository userTokenRepository;
  private final ApplicationEventPublisher eventPublisher;

  private final UserRegisterMapper userRegisterMapper;
  private final ClockService clockService;

  /** How long before activation token expires in hours. */
  @Value("${app.user.token.activation.expires}")
  private long activationTokenExpires;

  /**
   * Registers user in UserLand system.
   * @param userRegisterReq User registration request.
   * @return Created user.
   */
  @Transactional
  public User register(UserRegisterReq userRegisterReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    verifyRegistration(userRegisterReq);
    User user = createUserData(userRegisterReq, nowAt);
    user = userRepository.save(user);

    triggerEvent(user);
    return user;
  }

  /**
   * Verifies state of user, ensuring it is allowed to be registered in system.
   * @param userRegisterReq User registration request.
   */
  private void verifyRegistration(UserRegisterReq userRegisterReq) {
    if (userRepository.existsByEmail(userRegisterReq.email()))
      throw new UserEmailAlreadyExistsException(userRegisterReq.email());
  }

  /**
   * Triggers user registration event for anyone interested.
   * @param user User data.
   */
  private void triggerEvent(User user) {
    UserRegisteredEvent userRegisteredEvent = new UserRegisteredEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        user.getTokens().getFirst().getToken()
    );
    // Will trigger UserEmailService.sendActivationEmail().
    eventPublisher.publishEvent(userRegisteredEvent);
  }

  //

  /**
   * Create and fill user data.
   * @param userRegisterReq User registration request.
   * @return User data.
   */
  private User createUserData(UserRegisterReq userRegisterReq, LocalDateTime nowAt) {
    User user = userRegisterMapper.toEntity(userRegisterReq);
    // Simple fields like status or blocked are pre-filled already.
    user.setCreatedAt(nowAt);
    user.setModifiedAt(nowAt);
    user.addToken(createTokenData(nowAt, EnTokenType.ACTIVATE));
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.CREATED));
    return user;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Activate user that has token with given token string.
   * @param tokenActivateReq Token string.
   */
  @Transactional
  public void activate(TokenActivateReq tokenActivateReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(tokenActivateReq.token(), nowAt);
    User user = userToken.getUser();
    user.setModifiedAt(nowAt);
    user.setStatus(EnUserStatus.ACTIVE);
    user.getTokens().remove(userToken);
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.ACTIVATED));

    userRepository.save(user);
  }

  /**
   * Retrieve user token based on token string. Will throw exception if token is not found or is expired.
   * @param tokenStr Token string.
   */
  private UserToken resolveToken(String tokenStr, LocalDateTime nowAt) {
    UserToken userToken = userTokenRepository.findByTypeAndToken(EnTokenType.ACTIVATE, tokenStr)
        .orElseThrow(() -> new UserTokenMissingException(tokenStr));
    if (userToken.getExpiresAt().isBefore(nowAt)) throw new UserTokenExpiredException(tokenStr);
    return userToken;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Create and fill token data.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return User token entry.
   */
  private UserToken createTokenData(LocalDateTime nowAt, EnTokenType type) {
    UserToken token = new UserToken();
    token.setCreatedAt(nowAt);
    token.setExpiresAt(nowAt.plusHours(activationTokenExpires));
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
  private UserHistory createHistoryEvent(LocalDateTime nowAt, EnHistoryWhat what) {
    UserHistory event = new UserHistory();
    event.setUuid(securityGeneratorService.uuid());
    event.setCreatedAt(nowAt);
    event.setWho(EnHistoryWho.USER);
    event.setWhat(what);
    return event;
  }
}
