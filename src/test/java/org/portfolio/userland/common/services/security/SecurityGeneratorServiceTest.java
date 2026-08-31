package org.portfolio.userland.common.services.security;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link SecurityGeneratorService}.
 */
public class SecurityGeneratorServiceTest extends BaseIntegrationTest {
  @Autowired
  private SecurityGeneratorService securityGeneratorService;

  private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

  // //////////////////////////////////////////////////////////////////////////
  // token

  @Test
  public void tokenReturnsConfiguredLength() {
    // Act
    String token = securityGeneratorService.token();

    // Assert
    assertThat(token).as("Token should have configured length of 32").hasSize(32);
  }

  @Test
  public void tokenReturnsAlphanumericCharacters() {
    // Act
    String token = securityGeneratorService.token();

    // Assert
    assertThat(token).as("Token should contain only alphanumeric characters")
        .matches(ALPHANUMERIC_PATTERN);
  }

  @Test
  public void tokenRespectsDifferentConfiguredLength() {
    // Arrange
    int originalLength = (int) ReflectionTestUtils.getField(securityGeneratorService, "tokenLength");
    ReflectionTestUtils.setField(securityGeneratorService, "tokenLength", 16);

    try {
      // Act
      String token = securityGeneratorService.token();

      // Assert
      assertThat(token).as("Token should have length of 16 when configured").hasSize(16);
      assertThat(token).matches(ALPHANUMERIC_PATTERN);
    } finally {
      // Restore
      ReflectionTestUtils.setField(securityGeneratorService, "tokenLength", originalLength);
    }
  }

  // //////////////////////////////////////////////////////////////////////////
  // uuid

  @Test
  public void uuidReturnsNonNullValidUuid() {
    // Act
    UUID uuid = securityGeneratorService.uuid();

    // Assert
    assertThat(uuid).as("UUID should not be null").isNotNull();
    assertThat(uuid.toString()).as("UUID should be parseable").matches(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
  }
}
