package org.portfolio.userland.test.helpers.factories;

import lombok.RequiredArgsConstructor;
import org.instancio.Instancio;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.entities.EnHistoryWhat;
import org.portfolio.userland.features.user.entities.EnTokenType;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static org.instancio.Select.field;

/**
 * Generates users for tests.
 */
@Service
@RequiredArgsConstructor
public class UserFactory {
  private final UserHistoryFactory userHistoryFactory;
  private final UserTokenFactory userTokenFactory;
  private final UserJwtFactory userJwtFactory;

  private final SecurityGeneratorService securityGeneratorService;
  private final JwtService jwtService;

  private final ClockService clockService;
  private final PasswordEncoder passwordEncoder;

  /**
   * Generate pending user.
   * @param tokenStr Activation token. Can be null, will generate it.
   * @return User.
   */
  public User genUserPending(String tokenStr) {
    if (tokenStr == null) tokenStr = securityGeneratorService.token();

    User user = genBaseUser();
    userTokenFactory.genTokenEntry(user, EnTokenType.ACTIVATE, tokenStr);
    return user;
  }

  /**
   * Generate activated user.
   * @return User.
   */
  public User genUser() {
    User user = genBaseUser();
    user.setStatus(EnUserStatus.ACTIVE);
    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.ACTIVATED);
    return user;
  }

  /**
   * Generate activated user that is already logged in.
   * <p>You can get JWT string with <code>String token = expectedUser.getJwts().stream().toList().getFirst().getToken();</code>.</p>
   * @return User.
   */
  public User genUserLogged() {
    User user = genBaseUser();
    user.setStatus(EnUserStatus.ACTIVE);
    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.ACTIVATED);

    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.LOGIN);
    String token = jwtService.generateToken(user);
    userJwtFactory.genJwtEntry(user, token);
    return user;
  }

  /**
   * Generate base user.
   * @return User.
   */
  private User genBaseUser() {
    User user = new User();
    user.setCreatedAt(clockService.getNowUTC());
    user.setModifiedAt(clockService.getNowUTC());
    user.setUsername("Jane");
    user.setEmail("test@example.com");
    user.setPassword(passwordEncoder.encode("Password123!"));
    user.setLang("en");

    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.CREATED);
    return user;
  }

  //

  /**
   * Generate random user using Instancio.
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
        .set(field(User::getLang), "en")
        .set(field(User::getStatus), status)
        .set(field(User::getLocked), false)
        .ignore(field(User::getTokens)) // we fill it manually
        .ignore(field(User::getJwts)) // ditto
        .ignore(field(User::getHistory)) // ditto
        .ignore(field(User::getPermissions)) // ditto
        .create();

    if (EnUserStatus.PENDING.equals(status)) userTokenFactory.genTokenEntry(randomUser, EnTokenType.ACTIVATE, securityGeneratorService.token());

    userHistoryFactory.genHistoryEvent(randomUser, EnHistoryWhat.CREATED);
    if (EnUserStatus.ACTIVE.equals(status)) userHistoryFactory.genHistoryEvent(randomUser, EnHistoryWhat.ACTIVATED);
    return randomUser;
  }
}
