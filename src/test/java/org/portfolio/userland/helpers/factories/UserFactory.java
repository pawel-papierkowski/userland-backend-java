package org.portfolio.userland.helpers.factories;

import lombok.RequiredArgsConstructor;
import org.instancio.Instancio;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.data.EnHistoryWhat;
import org.portfolio.userland.features.user.data.EnTokenType;
import org.portfolio.userland.features.user.data.EnUserStatus;
import org.portfolio.userland.features.user.data.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static org.instancio.Select.field;

/**
 * Generates users for tests.
 * NOTE: if it becomes even more unwieldy, consider Instancio.
 */
@Service
@RequiredArgsConstructor
public class UserFactory {
  private final UserHistoryFactory userHistoryFactory;
  private final UserTokenFactory userTokenFactory;

  private final SecurityGeneratorService securityGeneratorService;

  private final ClockService clockService;
  private final PasswordEncoder passwordEncoder;

  /**
   * Generate pending user.
   * @param tokenStr Activation token. Can be null, will generate it.
   * @return User.
   */
  public User genPendingUserWithToken(String tokenStr) {
    if (tokenStr == null) tokenStr = securityGeneratorService.token();

    User user = new User();
    user.setCreatedAt(clockService.getNowUTC());
    user.setModifiedAt(clockService.getNowUTC());
    user.setUsername("Jane");
    user.setEmail("test@example.com");
    user.setPassword(passwordEncoder.encode("Password123!"));

    userTokenFactory.genTokenEntry(user, EnTokenType.ACTIVATE, tokenStr);
    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.CREATED);

    return user;
  }

  /**
   * Generate and persist activated user.
   * @return User.
   */
  public User genUser() {
    User user = new User();
    user.setCreatedAt(clockService.getNowUTC());
    user.setModifiedAt(clockService.getNowUTC());
    user.setUsername("Jane");
    user.setEmail("test@example.com");
    user.setPassword(passwordEncoder.encode("Password123!"));
    user.setStatus(EnUserStatus.ACTIVE);

    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.CREATED);
    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.ACTIVATED);

    return user;
  }

  //

  /**
   * Generate random user.
   * @param status Status of user.
   * @return User with randomized data.
   */
  public User genRandUser(EnUserStatus status) {
    User randomUser = Instancio.of(User.class)
        .ignore(field(User::getId)) // let Hibernate take care of that
        .set(field(User::getCreatedAt), clockService.getNowUTC())
        .set(field(User::getModifiedAt), clockService.getNowUTC())
        .generate(field(User::getEmail), gen -> gen.net().email())
        .set(field(User::getPassword), passwordEncoder.encode("Password123!"))
        .set(field(User::getStatus), status)
        .set(field(User::getBlocked), false)
        .ignore(field(User::getTokens)) // we fill it manually
        .ignore(field(User::getHistory)) // ditto
        .create();

    if (EnUserStatus.PENDING.equals(status)) userTokenFactory.genTokenEntry(randomUser, EnTokenType.ACTIVATE, securityGeneratorService.token());

    userHistoryFactory.genHistoryEvent(randomUser, EnHistoryWhat.CREATED);
    if (EnUserStatus.ACTIVE.equals(status)) userHistoryFactory.genHistoryEvent(randomUser, EnHistoryWhat.ACTIVATED);
    return randomUser;
  }
}
