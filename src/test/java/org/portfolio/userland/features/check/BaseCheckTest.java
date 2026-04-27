package org.portfolio.userland.features.check;

import org.portfolio.userland.features.user.repositories.UserJwtRepository;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.portfolio.userland.test.helpers.factories.user.UserFactory;
import org.portfolio.userland.test.helpers.factories.user.UserHistoryFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for any check test.
 */
public abstract class BaseCheckTest extends BaseIntegrationTest {
  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserJwtRepository userJwtRepository;

  @Autowired
  protected UserFactory userFactory;
  @Autowired
  protected UserHistoryFactory userHistoryFactory;

  //

  @Override
  protected void cleanDatabase() {
    super.cleanDatabase();
    userRepository.deleteAll(); // will remove everything from related tables too
  }
}
