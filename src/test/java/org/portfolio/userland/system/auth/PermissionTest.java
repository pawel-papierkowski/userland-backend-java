package org.portfolio.userland.system.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test of permission service.
 */
public class PermissionTest extends BaseUserTest {
  @Autowired
  private PermissionService permissionService;

  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void unloggedHasNoAccessToAdminPanel() {
    Boolean actualResult = permissionService.hasAccessToAdminPanel();
    Boolean expectedResult = false;
    assertThat(actualResult).as("Should NOT have access to admin panel.").isEqualTo(expectedResult);
  }

  @Test
  @WithMockCustomUser
  public void loggedHasNoAccessToAdminPanel() {
    Boolean actualResult = permissionService.hasAccessToAdminPanel();
    Boolean expectedResult = false;
    assertThat(actualResult).as("Currently logged-in user should NOT have access to admin panel.").isEqualTo(expectedResult);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void loggedHasAccessToAdminPanel() {
    Boolean actualResult = permissionService.hasAccessToAdminPanel();
    Boolean expectedResult = true;
    assertThat(actualResult).as("Currently logged-in user should have access to admin panel.").isEqualTo(expectedResult);
  }
}
