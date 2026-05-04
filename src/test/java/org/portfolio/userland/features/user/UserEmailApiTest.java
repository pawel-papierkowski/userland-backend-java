package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;

/**
 * Integration test for changing email for user account.
 */
public class UserEmailApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  public void requestEmailChange() throws Exception {
    // TODO
  }
}
