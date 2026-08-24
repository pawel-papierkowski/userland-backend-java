package org.portfolio.userland.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper class for string manipulation methods that aren't present in other libraries.
 */
public class StringHelper {
  private static final int LENGTH_TOKEN_VISIBLE = 13;
  private static final int LENGTH_JWT_VISIBLE = 23;

  private StringHelper() {
  }

  //

  /**
   * Masks token.
   * <p>Example: <code>oMVoUQeNa5CRS13dBvMSz1LwaSfFXMfpN</code> will be shortened to <code>oMVoUQeNa5...</code></p>
   * @param token Token to mask.
   * @return Masked token.
   */
  public static String maskToken(String token) {
    return StringUtils.abbreviate(token, LENGTH_TOKEN_VISIBLE);
  }

  /**
   * Masks JWT.
   * <p>Example: <code>eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiYWRtaW4iLCJuYW1lIjoiUGF3ZcWCIFBhcGllcmtvd3NraSIsInN1YiI6InBhd2VsLnBhcGllcmtvd3NraUBnbWFpbC5jb20iLCJpYXQiOjE3ODAwNjgzNzAsImV4cCI6MTc4MDE1NDc3MH0.1SqWUyiexH9WTLt8-LpovCk8UJ74dzUyw_f-Dop4kgA</code>
   * will be shortened to <code>eyJhbGciOi..._f-Dop4kgA</code></p>
   * @param jwt JWT to mask.
   * @return Masked JWT.
   */
  public static String maskJwt(String jwt) {
    return StringUtils.abbreviateMiddle(jwt, "...", LENGTH_JWT_VISIBLE);
  }
}
