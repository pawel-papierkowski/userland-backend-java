package org.portfolio.userland.test.helpers.gcp;

import io.jsonwebtoken.Jwts;
import org.portfolio.userland.config.security.JwtGcpTokenValidator;
import org.portfolio.userland.gcp.constants.GcpConst;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Utility for creating signed JWT tokens in GCP integration tests.
 * <p>Uses a static RSA keypair generated once per JVM. Tokens are signed with the private key,
 * and a matching {@link JwtDecoder} (configured with the public key) is provided for the security filter chain.</p>
 * <p>This allows testing the full OIDC auth chain without calling Google's real JWKS endpoint.</p>
 */
public final class GcpOidcTokenHelper {
  /** Test service account name (must match userland-gcp-test.yaml). */
  public static final String TEST_SERVICE_ACCOUNT = "test-sa";
  /** Test project ID (must match userland-gcp-test.yaml). */
  public static final String TEST_PROJECT = "test-project";
  /** Test service account email derived from account name and project. */
  public static final String TEST_SA_EMAIL = TEST_SERVICE_ACCOUNT + "@" + TEST_PROJECT + ".iam.gserviceaccount.com";
  /** Test service URL (must match userland-gcp-test.yaml). */
  public static final String TEST_SERVICE_URL = "https://test.example.com";
  /** Expected audience for the email send endpoint. */
  public static final String TEST_AUDIENCE = TEST_SERVICE_URL + GcpConst.EMAIL_SEND_ENDPOINT_PATH;

  /** Static RSA keypair, generated once for the entire test suite. */
  private static final KeyPair KEY_PAIR;
  /** Private key for signing tokens. */
  private static final RSAPrivateKey PRIVATE_KEY;
  /** Public key for verifying tokens. */
  private static final RSAPublicKey PUBLIC_KEY;

  static {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KEY_PAIR = generator.generateKeyPair();
      PRIVATE_KEY = (RSAPrivateKey) KEY_PAIR.getPrivate();
      PUBLIC_KEY = (RSAPublicKey) KEY_PAIR.getPublic();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("RSA algorithm not available", e);
    }
  }

  private GcpOidcTokenHelper() {}

  // //////////////////////////////////////////////////////////////////////////
  // Token builders
  // //////////////////////////////////////////////////////////////////////////

  /**
   * Creates a valid GCP OIDC token with correct audience and service account identity.
   * @return Signed JWT token string.
   */
  public static String validToken() {
    return validToken(Instant.now().plusSeconds(300));
  }

  /**
   * Creates a valid GCP OIDC token with correct audience and service account identity, expiring at given time.
   * @param expiresAt Token expiration instant.
   * @return Signed JWT token string.
   */
  public static String validToken(Instant expiresAt) {
    return buildToken(TEST_AUDIENCE, TEST_SA_EMAIL, expiresAt);
  }

  /**
   * Creates a token with wrong audience (different endpoint URL).
   * @return Signed JWT token string.
   */
  public static String wrongAudienceToken() {
    return buildToken("https://wrong-service.example.com/other", TEST_SA_EMAIL, Instant.now().plusSeconds(300));
  }

  /**
   * Creates a token with missing audience claim.
   * @return Signed JWT token string.
   */
  public static String missingAudienceToken() {
    return buildToken(null, TEST_SA_EMAIL, Instant.now().plusSeconds(300));
  }

  /**
   * Creates a token with wrong service account identity (different email claim).
   * @return Signed JWT token string.
   */
  public static String wrongIdentityToken() {
    return buildToken(TEST_AUDIENCE, "attacker@gmail.com", Instant.now().plusSeconds(300));
  }

  /**
   * Creates a token with missing email claim.
   * @return Signed JWT token string.
   */
  public static String missingIdentityToken() {
    return buildToken(TEST_AUDIENCE, null, Instant.now().plusSeconds(300));
  }

  /**
   * Creates an expired token (valid audience and identity, but past expiration).
   * @return Signed JWT token string.
   */
  public static String expiredToken() {
    return buildToken(TEST_AUDIENCE, TEST_SA_EMAIL, Instant.now().minusSeconds(60));
  }

  /**
   * Returns an intentionally malformed token string that cannot be parsed.
   * @return Malformed token string.
   */
  public static String malformedToken() {
    return "NOT_A_VALID_JWT_TOKEN";
  }

  // //////////////////////////////////////////////////////////////////////////
  // Decoder
  // //////////////////////////////////////////////////////////////////////////

  /**
   * Creates a {@link JwtDecoder} configured with the test public key and a locally constructed
   * {@link JwtGcpTokenValidator}. This decoder can be used as a {@code @Primary @Bean} override
   * in test configurations to replace the Google JWKS decoder.
   * <p>Issuer validation is intentionally skipped (already covered by unit tests).
   * The validator is created with test values matching {@code userland-gcp-test.yaml}.</p>
   * @return JwtDecoder configured for testing.
   */
  public static JwtDecoder createTestDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(PUBLIC_KEY).build();
    JwtGcpTokenValidator validator = new JwtGcpTokenValidator();
    ReflectionTestUtils.setField(validator, "serviceUrl", TEST_SERVICE_URL);
    ReflectionTestUtils.setField(validator, "serviceAccount", TEST_SERVICE_ACCOUNT);
    ReflectionTestUtils.setField(validator, "projectId", TEST_PROJECT);
    decoder.setJwtValidator(validator);
    return decoder;
  }

  // //////////////////////////////////////////////////////////////////////////
  // Internals
  // //////////////////////////////////////////////////////////////////////////

  /**
   * Builds and signs a JWT token with given claims.
   * @param audience Audience claim value, may be null.
   * @param email Email claim value, may be null.
   * @param expiresAt Expiration instant.
   * @return Signed JWT token string.
   */
  private static String buildToken(String audience, String email, Instant expiresAt) {
    var builder = Jwts.builder()
        .subject("test-subject")
        .issuedAt(Date.from(Instant.now().minusSeconds(10)))
        .expiration(Date.from(expiresAt))
        .signWith(PRIVATE_KEY);

    if (audience != null) builder = builder.claim("aud", List.of(audience));
    if (email != null) builder = builder.claim("email", email);

    return builder.compact();
  }
}
