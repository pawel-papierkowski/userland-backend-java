package org.portfolio.userland.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.gcp.constants.GcpConst;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests validation of GCP OIDC tokens: audience must match this service's endpoint, and identity ('email' claim)
 * must be our service account. These checks are what prevents arbitrary Google-signed tokens from authenticating
 * against internal GCP endpoints.
 */
public class JwtGcpTokenValidatorTest {
  private static final String SERVICE_URL = "https://userland.example.com";
  private static final String EXPECTED_AUDIENCE = SERVICE_URL + GcpConst.EMAIL_SEND_ENDPOINT_PATH;
  private static final String SA_NAME = "test-sa";
  private static final String PROJECT_ID = "test-project";
  private static final String EXPECTED_EMAIL = SA_NAME + "@" + PROJECT_ID + ".iam.gserviceaccount.com";

  private JwtGcpTokenValidator validator;

  /**
   * Prepares validator with test configuration values (normally injected by Spring).
   */
  @BeforeEach
  public void setUp() {
    validator = new JwtGcpTokenValidator();
    ReflectionTestUtils.setField(validator, "serviceUrl", SERVICE_URL);
    ReflectionTestUtils.setField(validator, "serviceAccount", SA_NAME);
    ReflectionTestUtils.setField(validator, "projectId", PROJECT_ID);
  }

  /**
   * Creates token with given claims, always with minimal valid structure.
   * @param audience Audience claim value(s), may be null to simulate missing claim.
   * @param email Email claim value, may be null to simulate missing claim.
   * @return Built JWT.
   */
  private Jwt createToken(List<String> audience, String email) {
    Jwt.Builder builder = Jwt.withTokenValue("token-value")
        .header("alg", "RS256")
        .subject("irrelevant")
        .issuedAt(Instant.now().minusSeconds(60))
        .expiresAt(Instant.now().plusSeconds(300));
    if (audience != null) builder = builder.audience(audience);
    if (email != null) builder = builder.claim("email", email);
    return builder.build();
  }

  //

  /**
   * Verifies that a proper Cloud Tasks token (correct audience + our SA as issuer identity) is accepted.
   */
  @Test
  public void acceptsValidServiceAccountToken() {
    OAuth2TokenValidatorResult result = validator.validate(createToken(List.of(EXPECTED_AUDIENCE), EXPECTED_EMAIL));
    assertThat(result.getErrors()).isEmpty();
  }

  /**
   * Verifies that token for a different endpoint URL (wrong audience) is rejected.
   */
  @Test
  public void rejectsWrongAudience() {
    OAuth2TokenValidatorResult result =
        validator.validate(createToken(List.of("https://other-service.example.com/whatever"), EXPECTED_EMAIL));
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().iterator().next().getDescription()).contains("audience");
  }

  /**
   * Verifies that token without any audience is rejected.
   */
  @Test
  public void rejectsMissingAudience() {
    OAuth2TokenValidatorResult result = validator.validate(createToken(null, EXPECTED_EMAIL));
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().iterator().next().getDescription()).contains("audience");
  }

  /**
   * Verifies that token minted for someone else (e.g. random Gmail account) is rejected - this is the core
   * protection against the open-relay scenario.
   */
  @Test
  public void rejectsForeignIdentity() {
    OAuth2TokenValidatorResult result =
        validator.validate(createToken(List.of(EXPECTED_AUDIENCE), "attacker@gmail.com"));
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().iterator().next().getDescription()).contains("service account");
  }

  /**
   * Verifies that token without 'email' claim is rejected.
   */
  @Test
  public void rejectsMissingIdentityClaim() {
    OAuth2TokenValidatorResult result = validator.validate(createToken(List.of(EXPECTED_AUDIENCE), null));
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().iterator().next().getDescription()).contains("service account");
  }
}
