package org.portfolio.userland.features.user;

import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.features.user.repositories.PermissionRepository;
import org.portfolio.userland.features.user.repositories.UserHistoryRepository;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.portfolio.userland.test.helpers.asserts.UserAssert;
import org.portfolio.userland.test.helpers.factories.UserFactory;
import org.portfolio.userland.test.helpers.factories.UserHistoryFactory;
import org.portfolio.userland.test.helpers.factories.UserPermissionFactory;
import org.portfolio.userland.test.helpers.factories.UserTokenFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class BaseUserTest extends BaseIntegrationTest {
  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserTokenRepository userTokenRepository;
  @Autowired
  protected UserHistoryRepository userHistoryRepository;
  @Autowired
  protected PermissionRepository permissionRepository;

  @Autowired
  protected UserFactory userFactory;
  @Autowired
  protected UserHistoryFactory userHistoryFactory;
  @Autowired
  protected UserTokenFactory userTokenFactory;
  @Autowired
  protected UserPermissionFactory userPermissionFactory;

  @Autowired
  protected UserAssert userAssert;

  @MockitoBean
  protected EmailService emailService;

  //

  protected void cleanDatabase() {
    // Clean up the database after every test so tests don't interfere with each other.
    userTokenRepository.deleteAll();
    userHistoryRepository.deleteAll();
    userRepository.deleteAll();
  }
}
