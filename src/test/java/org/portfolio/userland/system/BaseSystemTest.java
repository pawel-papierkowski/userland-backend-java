package org.portfolio.userland.system;

import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.portfolio.userland.test.helpers.asserts.UserAssert;
import org.portfolio.userland.test.helpers.factories.UserFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for any system test.
 */
public abstract class BaseSystemTest extends BaseIntegrationTest {

  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserFactory userFactory;
  @Autowired
  protected UserAssert userAssert;
}
