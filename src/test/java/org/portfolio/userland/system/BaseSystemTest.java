package org.portfolio.userland.system;

import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.portfolio.userland.test.helpers.asserts.SystemHistoryAssert;
import org.portfolio.userland.test.helpers.asserts.UserAssert;
import org.portfolio.userland.test.helpers.factories.system.SystemHistoryFactory;
import org.portfolio.userland.test.helpers.factories.user.UserFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for any system test.
 */
public abstract class BaseSystemTest extends BaseIntegrationTest {
  @Autowired
  protected SystemHistoryFactory systemHistoryFactory;
  @Autowired
  protected SystemHistoryAssert systemHistoryAssert;

  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserFactory userFactory;
  @Autowired
  protected UserAssert userAssert;
}
