package org.portfolio.userland.config.security.constants;

/** Endpoint-related constants. */
public class EndpointConst {
  /** Endpoints for Swagger. */
  public static final String[] SWAGGER = new String[] {
      "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
  };

  /** Endpoints that do not require any log in. */
  public static final String[] PUBLIC = new String[] {
      "/api/checks/alive", // alive check
      "/api/checks/pretendWork", // pretend work check
      "/api/checks/exception", // exception check
      "/api/users/register", // user registration
      "/api/users/activate", // activate user account
      "/api/users/password/*", // reset password
      "/api/users/login", // login
      "/api/users/logout" // logout
  };

  /** Endpoints for administration panel. */
  public static final String[] ADMIN = new String[] {
      "/api/admin/*"  // any administration panel endpoint
  };

  /** Endpoints for system. */
  public static final String[] SYSTEM = new String[] {
      "/api/system/*"  // any system endpoint
  };

  /** Endpoints for GCP. */
  public static final String[] GCP = new String[] {
      "/api/gcp/**"  // all GCP endpoints
  };
}
