package org.portfolio.userland.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests string masking helpers. Note these are pure static utility methods, so no Spring context is needed.
 */
public class StringHelperTest {
  /** Example token used in tests. */
  private static final String TOKEN = "oMVoUQeNa5CRS13dBvMSz1LwaSfFXMfpN";
  /** Example JWT used in tests. */
  private static final String JWT = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiYWRtaW4iLCJuYW1lIjoiUGF3ZcWCIFBhcGllcmtvd3NraSIsInN1YiI6InBhd2VsLnBhcGllcmtvd3NraUBnbWFpbC5jb20iLCJpYXQiOjE3ODAwNjgzNzAsImV4cCI6MTc4MDE1NDc3MH0.1SqWUyiexH9WTLt8-LpovCk8UJ74dzUyw_f-Dop4kgA";

  // MASK TOKEN

  /**
   * Verifies that long token is shortened to first few characters followed by ellipsis.
   */
  @Test
  public void maskTokenShortensLongToken() {
    String result = StringHelper.maskToken(TOKEN);

    assertThat(result).isEqualTo("oMVoUQeNa5...");
  }

  /**
   * Verifies that token at or below visible-length threshold is returned unchanged.
   */
  @Test
  public void maskTokenKeepsShortTokenUnchanged() {
    assertThat(StringHelper.maskToken("short")).isEqualTo("short"); // below limit
    assertThat(StringHelper.maskToken("1234567890123")).isEqualTo("1234567890123"); // exactly at limit
    assertThat(StringHelper.maskToken("12345678901234")).isEqualTo("1234567890..."); // slightly above limit
  }

  /**
   * Verifies safe handling of null and empty inputs.
   */
  @Test
  public void maskTokenHandlesNullAndEmpty() {
    assertThat(StringHelper.maskToken(null)).isNull();
    assertThat(StringHelper.maskToken("")).isEmpty();
  }

  /**
   * Verifies that masked token does not reveal most of the original value.
   */
  @Test
  public void maskTokenHidesContent() {
    String result = StringHelper.maskToken(TOKEN);

    assertThat(result).isNotEqualTo(TOKEN);
    assertThat(TOKEN).contains(result.replace("...", ""));
    // Ellipsis must be present so reader knows value was cut.
    assertThat(result).endsWith("...");
  }

  // Mask JWT

  /**
   * Verifies that long JWT is shortened to beginning, ellipsis and end (middle removed).
   */
  @Test
  public void maskJwtShortensLongJwt() {
    String result = StringHelper.maskJwt(JWT);

    // abbreviateMiddle(maxWidth=23) keeps first 10 and last 10 characters.
    assertThat(result).isEqualTo("eyJhbGciOi..._f-Dop4kgA");
    // Beginning AND end must survive - both are useful for log correlation.
    assertThat(result).startsWith("eyJhbGciOi");
    assertThat(result).endsWith("_f-Dop4kgA");
  }

  /**
   * Verifies that JWT at or below visible-length threshold is returned unchanged.
   */
  @Test
  public void maskJwtKeepsShortJwtUnchanged() {
    assertThat(StringHelper.maskJwt("abc.def")).isEqualTo("abc.def"); // below limit
    assertThat(StringHelper.maskJwt("12345678901234567890123")).isEqualTo("12345678901234567890123"); // exactly at limit
    assertThat(StringHelper.maskJwt("123456789012345678901234")).isEqualTo("1234567890...5678901234"); // slightly above limit
  }

  /**
   * Verifies safe handling of null and empty inputs.
   */
  @Test
  public void maskJwtHandlesNullAndEmpty() {
    assertThat(StringHelper.maskJwt(null)).isNull();
    assertThat(StringHelper.maskJwt("")).isEmpty();
  }
}
