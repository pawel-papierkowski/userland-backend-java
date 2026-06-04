package org.portfolio.userland.features.user;

import org.portfolio.userland.features.email.services.EmailService;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.mappers.UserMapper;
import org.portfolio.userland.features.user.repositories.config.UserConfigRepository;
import org.portfolio.userland.features.user.repositories.history.UserHistoryRepository;
import org.portfolio.userland.features.user.repositories.jwt.UserJwtRepository;
import org.portfolio.userland.features.user.repositories.permission.PermissionRepository;
import org.portfolio.userland.features.user.repositories.permission.UserPermissionRepository;
import org.portfolio.userland.features.user.repositories.token.UserTokenRepository;
import org.portfolio.userland.features.user.repositories.user.UserProfileRepository;
import org.portfolio.userland.features.user.repositories.user.UserRepository;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.portfolio.userland.test.helpers.asserts.JwtAssert;
import org.portfolio.userland.test.helpers.asserts.UserAdminAssert;
import org.portfolio.userland.test.helpers.asserts.UserAssert;
import org.portfolio.userland.test.helpers.asserts.UserProfileAssert;
import org.portfolio.userland.test.helpers.factories.user.*;
import org.portfolio.userland.test.helpers.factories.user.admin.UserAdminFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for any user test.
 */
@RecordApplicationEvents
public abstract class BaseUserTest extends BaseIntegrationTest {
  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserProfileRepository userProfileRepository;
  @Autowired
  protected UserConfigRepository userConfigRepository;
  @Autowired
  protected UserHistoryRepository userHistoryRepository;
  @Autowired
  protected UserTokenRepository userTokenRepository;
  @Autowired
  protected UserJwtRepository userJwtRepository;
  @Autowired
  protected UserPermissionRepository userPermissionRepository;
  @Autowired
  protected PermissionRepository permissionRepository;

  @Autowired
  protected UserFactory userFactory;
  @Autowired
  protected UserProfileFactory userProfileFactory;
  @Autowired
  protected UserConfigFactory userConfigFactory;
  @Autowired
  protected UserHistoryFactory userHistoryFactory;
  @Autowired
  protected UserTokenFactory userTokenFactory;
  @Autowired
  protected UserJwtFactory userJwtFactory;
  @Autowired
  protected UserPermissionFactory userPermissionFactory;

  @Autowired
  protected UserAdminFactory userAdminFactory;

  @Autowired
  protected UserMapper userMapper;

  @Autowired
  protected UserAssert userAssert;
  @Autowired
  protected UserProfileAssert userProfileAssert;
  @Autowired
  protected JwtAssert jwtAssert;
  @Autowired
  protected UserAdminAssert userAdminAssert;

  @MockitoBean
  protected EmailService emailService;

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // IDE cannot find it since it requires @RecordApplicationEvents
  protected ApplicationEvents applicationEvents;

  //

  @Override
  protected void cleanDatabase() {
    super.cleanDatabase();
    userRepository.deleteAll(); // will remove everything from related tables too, as these have cascading deletes
  }

  //

  /**
   * Assert all user-related data.
   * @param email Email of user.
   * @param expectedUser Expected user.
   * @param expectedUserProfile Expected user profile. If null, skip asserting profile.
   * @return Actual user.
   */
  protected User assertAllUser(String email, User expectedUser, UserProfile expectedUserProfile) {
    boolean userExists = userRepository.existsByEmail(email);
    assertThat(userExists).as("User should exist").isTrue();

    User actualUser = userRepository.findByEmail(email).orElseThrow();
    userAssert.assertIt(actualUser, expectedUser);

    if (expectedUserProfile != null) {
      UserProfile actualUserProfile = userProfileRepository.findById(actualUser.getId()).orElseThrow();
      userProfileAssert.assertIt(actualUserProfile, expectedUserProfile);
    }

    return actualUser;
  }
}
