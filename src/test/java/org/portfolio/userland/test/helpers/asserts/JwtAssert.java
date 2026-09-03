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
   * Assert that JWT's claims are valid.
   * @param jwt JWT token.
   * @param expectedClaimMap Expected claims.
   */
  public void assertIt(String jwt, Map<String, Object> expectedClaimMap) {
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(jwt);
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }
}
