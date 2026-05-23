package org.portfolio.userland.config.security.constants;

/** CORS-related constants. */
public class CorsConst {
  /** We allow local Vue frontend and frontend on GitHub Pages. */
  public final static String[] ALLOWED_ORIGINS = {
      "http://localhost:5173", // local development frontend
      "https://pawelpapierkowski.net.pl" // production frontend
  };

  /** We allow pretty much all HTTP methods. */
  public final static String[] ALLOWED_METHODS = { "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS" };

  /** We allow certain headers. */
  public final static String[] ALLOWED_HEADERS = { "Authorization", "Content-Type", "X-Requested-With" };
}
