package org.portfolio.userland.system.auth.jwt.constants;

/**
 * Constants for JWT claims.
 */
public class JwtClaims {
  private JwtClaims() {
  }

  // Standard claims.

  /** Issued at as long. */
  public final static String ISSUED = "iat";
  /** Expires at as long. */
  public final static String EXPIRES = "exp";
  /** Subject as string. Note it is user account email. */
  public final static String SUBJECT = "sub";

  // Custom claims.

  /** Username as string. */
  public final static String NAME = "name";
  /** Permissions as map of strings. */
  public final static String PERMS = "perms";
}
