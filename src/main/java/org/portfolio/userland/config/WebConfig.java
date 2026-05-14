package org.portfolio.userland.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration. Mainly for CORS stuff.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
  // We allow local Vue server and frontend on GitHub Pages.
  private final static String[] ALLOWED_ORIGINS = {
      "http://localhost:5173", // local development frontend
      "https://pawelpapierkowski.net.pl" // production frontend
  };

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(ALLOWED_ORIGINS)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
