package org.portfolio.userland.system.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.system.auth.data.EnPermKind;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

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
    Boolean actualResult = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL);
    Boolean expectedResult = false;
    assertThat(actualResult).as("Should NOT have access to admin panel.").isEqualTo(expectedResult);
  }

  @Test
  @WithMockCustomUser
  public void loggedHasNoAccessToAdminPanel() {
    Boolean actualResult = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL);
    Boolean expectedResult = false;
    assertThat(actualResult).as("Currently logged-in user should NOT have access to admin panel.").isEqualTo(expectedResult);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void loggedHasAccessToAdminPanel() {
    Boolean actualResult = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL);
    Boolean expectedResult = true;
    assertThat(actualResult).as("Currently logged-in user should have access to admin panel.").isEqualTo(expectedResult);
  }

  @Test
  @WithMockCustomUser(authorities = { "POST_EDIT" })
  public void loggedHasOtherPermissions() {
    Boolean actualResult = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL);
    Boolean expectedResult = false;
    assertThat(actualResult).as("Currently logged-in user should NOT have access to admin panel.").isEqualTo(expectedResult);
  }

  //

  @Test
  public void hasCustomPermissions() {
    // create manually
    Permission permPost = new Permission();
    permPost.setName("POST");
    UserPermission userPermission = new UserPermission();
    userPermission.setPermission(permPost);
    userPermission.setValue("edit");

    Boolean actualResult = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL, Set.of(userPermission));
    Boolean expectedResult = false;
    assertThat(actualResult).as("Given permissions should NOT have access to admin panel.").isEqualTo(expectedResult);
  }
}
