package org.portfolio.userland.system.auth.jwt;

import io.jsonwebtoken.io.Decoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.portfolio.userland.common.exception.SystemMisconfigurationException;
import org.portfolio.userland.common.services.clock.ClockService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests fail-fast startup validation of the JWT secret in <code>JwtService</code>.
 * Ensures the application refuses to boot when the secret is missing, set to the committed
 * publicly known placeholder, malformed or too short - otherwise tokens could be forged.
 */
public class JwtSecretValidationTest {
  /** Secret used as valid baseline - random 32 bytes encoded in BASE64. */
  private static final String VALID_SECRET = "qXqyG/cXonpbJY7zfChA/XoatblyieXumWmRg3mYjUc=";

  private JwtService jwtService;

  /**
   * Prepares service instance without Spring context; secret is injected per test.
   */
  @BeforeEach
  public void setUp() {
    jwtService = new JwtService(new JwtClock(Mockito.mock(ClockService.class)));
  }

  /**
   * Verifies that initialization fails when secret equals the committed placeholder value.
   */
  @Test
  public void errPlaceholderSecret() {
    ReflectionTestUtils.setField(jwtService, "secretKey", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

    Throwable thrown = catchThrowable(() -> ReflectionTestUtils.invokeMethod(jwtService, "initSigningAssets"));

    assertThat(thrown).isInstanceOf(SystemMisconfigurationException.class);
    assertThat(thrown.getMessage()).contains("placeholder");
  }

  /**
   * Verifies that initialization fails when secret is blank or missing entirely.
   */
  @Test
  public void errBlankSecret() {
    // Arrange & Act & Assert: empty string.
    ReflectionTestUtils.setField(jwtService, "secretKey", "");
    Throwable thrownEmpty = catchThrowable(() -> ReflectionTestUtils.invokeMethod(jwtService, "initSigningAssets"));
    assertThat(thrownEmpty).isInstanceOf(SystemMisconfigurationException.class);

    // Arrange & Act & Assert: whitespace-only string.
    ReflectionTestUtils.setField(jwtService, "secretKey", "   ");
    Throwable thrownWhitespace = catchThrowable(() -> ReflectionTestUtils.invokeMethod(jwtService, "initSigningAssets"));
    assertThat(thrownWhitespace).isInstanceOf(SystemMisconfigurationException.class);

    // Arrange & Act & Assert: null (property not resolved at all).
    ReflectionTestUtils.setField(jwtService, "secretKey", null);
    Throwable thrownNull = catchThrowable(() -> ReflectionTestUtils.invokeMethod(jwtService, "initSigningAssets"));
    assertThat(thrownNull).isInstanceOf(SystemMisconfigurationException.class);
  }

  /**
   * Verifies that initialization fails when decoded secret is shorter than required 256 bits.
   */
  @Test
  public void errTooShortSecret() {
    // Arrange: valid BASE64 of only 16 bytes.
    String shortSecret = java.util.Base64.getEncoder().encodeToString(new byte[16]);
    ReflectionTestUtils.setField(jwtService, "secretKey", shortSecret);

    // Act & Assert.
    Throwable thrown = catchThrowable(() -> ReflectionTestUtils.invokeMethod(jwtService, "initSigningAssets"));
    assertThat(thrown).isInstanceOf(SystemMisconfigurationException.class);
    assertThat(thrown.getMessage()).contains("256 bits");
  }

  /**
   * Verifies that initialization fails when secret is not valid BASE64.
   */
  @Test
  public void errMalformedSecret() {
    ReflectionTestUtils.setField(jwtService, "secretKey", "this-is-not-base64!!!");

    Throwable thrown = catchThrowable(() -> ReflectionTestUtils.invokeMethod(jwtService, "initSigningAssets"));

    assertThat(thrown).isInstanceOf(SystemMisconfigurationException.class);
    assertThat(thrown.getMessage()).contains("BASE64");
  }

  /**
   * Verifies that initialization succeeds with proper secret and produces usable signing assets.
   */
  @Test
  public void validSecretInitializes() {
    ReflectionTestUtils.setField(jwtService, "secretKey", VALID_SECRET);

    ReflectionTestUtils.invokeMethod(jwtService, "initSigningAssets");

    Object signingKey = ReflectionTestUtils.getField(jwtService, "signingKey");
    Object jwtParser = ReflectionTestUtils.getField(jwtService, "jwtParser");
    assertThat(signingKey).as("Signing key must be initialized").isNotNull();
    assertThat(jwtParser).as("JWT parser must be initialized").isNotNull();
    // Sanity check: secret decodes to exactly the required key length.
    assertThat(Decoders.BASE64.decode(VALID_SECRET)).hasSize(32);
  }
}
