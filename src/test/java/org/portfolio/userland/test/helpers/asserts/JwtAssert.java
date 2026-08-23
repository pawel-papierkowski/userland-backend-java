package org.portfolio.userland.test.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts JWT token.
 */
@Service
@RequiredArgsConstructor
public class JwtAssert {
  protected final JwtService jwtService;

  /**
   * Assert that JWT is valid.
   * @param jwt JWT token.
   * @param email Email (sub).
   * @param expectedClaimMap Expected claims.
   */
  public void assertIt(String jwt, String email, Map<String, Object> expectedClaimMap) {
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(jwt);
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }
}
