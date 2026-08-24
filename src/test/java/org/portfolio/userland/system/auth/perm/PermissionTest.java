package org.portfolio.userland.system.auth.perm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
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

  @BeforeEach
  public void setup() {
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

  /**
   * Regression test: entity-based overload must be case-insensitive, so data stored with different casing (like
   * 'ADMIN' instead of 'admin') still matches. This keeps it consistent with authority-based checks, where
   * permission strings are uppercased before comparison.
   */
  @Test
  public void hasCustomPermissionsIsCaseInsensitive() {
    Permission permRole = new Permission();
    permRole.setName("ROLE"); // wrong case on purpose
    UserPermission userPermission = new UserPermission();
    userPermission.setPermission(permRole);
    userPermission.setValue("ADMIN"); // wrong case on purpose

    Boolean actualResult = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL, Set.of(userPermission));
    Boolean expectedResult = true;
    assertThat(actualResult).as("Mixed-case permission should grant access to admin panel.").isEqualTo(expectedResult);
  }

  @Test
  public void hasCustomPermissionsDeniesUnknownValuesRegardlessOfCase() {
    Permission permRole = new Permission();
    permRole.setName("role");
    UserPermission userPermission = new UserPermission();
    userPermission.setPermission(permRole);
    userPermission.setValue("BOSS"); // unknown value, wrong case on purpose

    Boolean actualResult = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL, Set.of(userPermission));
    Boolean expectedResult = false;
    assertThat(actualResult).as("Unknown value must NOT grant access to admin panel.").isEqualTo(expectedResult);
  }

  @Test
  public void bothOverloadsAgreeForMixedCaseData() {
    // The same permission set fed through the entity-based overload and through the authority-based overload
    // (authorities built the same way as during login/JWT claim resolution) must produce identical verdicts.
    Permission permRole = new Permission();
    permRole.setName("ROLE");
    UserPermission userPermission = new UserPermission();
    userPermission.setPermission(permRole);
    userPermission.setValue("ADMIN");

    Set<UserPermission> permissions = Set.of(userPermission);

    // Build user details the same way CustomUserDetailsService does from DB permissions.
    CustomUserDetails customUserDetails = new CustomUserDetails(1L, true, false, "Jan Kowalski", "jan@test.com",
        PermissionHelper.resolveAuthorities(permissions));

    boolean viaEntities = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL, permissions);
    boolean viaAuthorities = permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL, customUserDetails);

    assertThat(viaAuthorities).as("Both overloads must agree for mixed-case data").isEqualTo(viaEntities);
    assertThat(viaEntities).as("Mixed-case role admin should grant access to admin panel").isTrue();
  }
}
