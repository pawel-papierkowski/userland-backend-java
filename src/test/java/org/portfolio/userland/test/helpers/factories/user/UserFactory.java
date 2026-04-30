package org.portfolio.userland.test.helpers.factories.user;

import lombok.RequiredArgsConstructor;
import org.instancio.Instancio;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.repositories.PermissionRepository;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.instancio.Select.field;

/**
 * Generates users for tests.
 */
@Service
@RequiredArgsConstructor
public class UserFactory extends BaseFactory {
  private final UserHistoryFactory userHistoryFactory;
  private final UserTokenFactory userTokenFactory;
  private final UserJwtFactory userJwtFactory;
  private final UserPermissionFactory userPermissionFactory;

  private final PermissionRepository permissionRepository;

  private final JwtService jwtService;

  private final PasswordEncoder passwordEncoder;

  /**
   * Generate user with deterministic data.
   * @param status Status of user.
   * @return User.
   */
  public User genUser(EnUserStatus status) {
    return genUser(status, Map.of());
  }

  /**
   * Generate user with deterministic data.
   * @param status Status of the user.
   * @param permissions Permissions of the user.
   * @return User.
   */
  public User genUser(EnUserStatus status, Map<String, String> permissions) {
    User user = genBaseUser(status);
    modifyStatus(user);
    addPermissions(user, permissions);
    return user;
  }

  /**
   * Generate activated user that is already logged in.
   * <p>You can get JWT string with <code>String token = expectedUser.getJwts().stream().toList().getFirst().getToken();</code>.</p>
   * @return User.
   */
  public User genUserLogged() {
    return genUserLogged(Map.of());
  }

  /**
   * Generate activated user that is already logged in.
   * <p>You can get JWT string with <code>String token = expectedUser.getJwts().stream().toList().getFirst().getToken();</code>.</p>
   * @param permissions Permissions of the user.
   * @return User.
   */
  public User genUserLogged(Map<String, String> permissions) {
    User user = genBaseUser(EnUserStatus.ACTIVE);
    modifyStatus(user);
    modifyLogged(user);
    addPermissions(user, permissions);
    return user;
  }

  /**
   * Generate base user.
   * @return User.
   */
  private User genBaseUser(EnUserStatus status) {
    return genBaseUser(status, "Jane", "test@example.com");
  }

  /**
   * Generate base user.
   * @param username Username.
   * @param email Email.
   * @return User.
   */
  private User genBaseUser(EnUserStatus status, String username, String email) {
    User user = new User();
    user.setCreatedAt(clockService.getNowUTC());
    user.setModifiedAt(clockService.getNowUTC());
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode("Password123!"));
    user.setLang("en");
    user.setStatus(status);

    userHistoryFactory.genHistoryEvent(user, EnUserHistoryWhat.CREATE, "");
    return user;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Generate random user.
   * @param status Status of user.
   * @return User with randomized data.
   */
  public User genRandUser(EnUserStatus status) {
    User randomUser = genBaseRandUser(status);
    modifyStatus(randomUser);
    return randomUser;
  }

  /**
   * Generate random user that is already logged in.
   * @return User with randomized data.
   */
  public User genRandUserLogged() {
    return genRandUserLogged(Map.of());
  }

  /**
   * Generate random user that is already logged in.
   * @return User with randomized data.
   */
  public User genRandUserLogged(Map<String, String> permissions) {
    User randomUser = genBaseRandUser(EnUserStatus.ACTIVE);
    modifyStatus(randomUser);
    modifyLogged(randomUser);
    addPermissions(randomUser, permissions);
    return randomUser;
  }

  /**
   * Generates base random user using Instancio.
   * @param status User status.
   * @return User with random data. Note collections are not touched.
   */
  private User genBaseRandUser(EnUserStatus status) {
    User user = Instancio.of(User.class)
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
    // all other fields are filled randomly
    userHistoryFactory.genHistoryEvent(user, EnUserHistoryWhat.CREATE, "");
    return user;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Modify user according to status.
   * @param user User to modify.
   */
  private void modifyStatus(User user) {
    EnUserStatus status = user.getStatus();
    if (EnUserStatus.PENDING.equals(status)) userTokenFactory.genTokenEntry(user, EnUserTokenType.ACTIVATE, null);
    if (EnUserStatus.ACTIVE.equals(status)) userHistoryFactory.genHistoryEvent(user, EnUserHistoryWhat.ACTIVATE, "");
  }

  /**
   * Modify user to be logged in.
   * @param user User to modify.
   */
  private void modifyLogged(User user) {
    userHistoryFactory.genHistoryEvent(user, EnUserHistoryWhat.LOGIN, "");
    String token = jwtService.generateToken(user);
    userJwtFactory.genJwtEntry(user, token);
  }

  /**
   * Add given permissions to user.
   * @param user User to modify.
   */
  private void addPermissions(User user, Map<String, String> permissions) {
    for (Map.Entry<String, String> entry : permissions.entrySet()) {
      addPermission(user, entry.getKey(), entry.getValue());
    }
  }

  /**
   * Add given permission to user.
   * @param user User to modify.
   * @param permName Permission name.
   * @param permValue Permission value.
   */
  private void addPermission(User user, String permName, String permValue) {
    Permission permissionRole = permissionRepository.findByName(permName).orElseThrow();
    userPermissionFactory.genPermissionEntry(user, permissionRole, permValue);
  }
}
