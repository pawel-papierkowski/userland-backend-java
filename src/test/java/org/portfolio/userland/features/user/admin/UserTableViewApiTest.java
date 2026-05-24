package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;

/**
 * Integration test for user table viewing.
 */
public class UserTableViewApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  /** Create user data for testing. */
  private void arrangeUserData() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // TODO actually arrange it
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewTable() throws Exception {
    arrangeUserData();
    // TODO actually test it
  }
}
