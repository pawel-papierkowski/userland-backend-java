package org.portfolio.userland.system.auth.perm;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of permission helper, especially conversion between JWT <code>perms</code> claim and authorities.
 */
public class PermissionHelperTest {

  /**
   * Resolves authority strings from given authorities collection.
   * @param authorities Authorities.
   * @return Authority strings.
   */
  private static List<String> resolveStrings(Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream().map(GrantedAuthority::getAuthority).toList();
  }

  //

  @Test
  public void resolvesAuthoritiesFromClaim() {
    // "role" -> "admin,operator" is exactly what resolvePermissions() produces for such permissions.
    Collection<? extends GrantedAuthority> result = PermissionHelper.resolveAuthoritiesFromClaim(
        Map.of("role", "admin,operator", "user", "view"));

    assertThat(resolveStrings(result))
        .as("Claim values should be converted to NAME_VALUE authorities")
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERATOR", "USER_VIEW");
    // Sorted by natural key required by UserDetails.
    assertThat(resolveStrings(result)).isSorted();
  }

  @Test
  public void resolvesSingleValueFromClaim() {
    Collection<? extends GrantedAuthority> result = PermissionHelper.resolveAuthoritiesFromClaim(
        Map.of("post", "edit"));

    assertThat(resolveStrings(result)).containsExactly("POST_EDIT");
  }

  @Test
  public void emptyAndNullClaimsResultInNoAuthorities() {
    assertThat(PermissionHelper.resolveAuthoritiesFromClaim(Map.of())).isEmpty();
    assertThat(PermissionHelper.resolveAuthoritiesFromClaim(null)).isEmpty();
    assertThat(PermissionHelper.resolveAuthoritiesFromClaim(Map.of("user", ""))).isEmpty();
  }

  //

  /**
   * Single source of truth for authority string format - mixed-case input must produce canonical uppercase
   * NAME_VALUE form.
   */
  @Test
  public void buildAuthorityNormalizesCase() {
    assertThat(PermissionHelper.buildAuthority("role", "AdMiN"))
        .as("Mixed-case input should produce canonical authority")
        .isEqualTo("ROLE_ADMIN");
    assertThat(PermissionHelper.buildAuthority("user", "edit")).isEqualTo("USER_EDIT");
  }
}
