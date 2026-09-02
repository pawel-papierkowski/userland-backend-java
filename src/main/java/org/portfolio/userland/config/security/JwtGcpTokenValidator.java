package org.portfolio.userland.config.security;

import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.gcp.constants.GcpConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator for JWT tokens used by Google Cloud (e.g. Cloud Tasks) to call our internal endpoints.
 * <p>Plain issuer validation is not enough: <b>any</b> valid Google-signed ID token (from any account or project)
 * would pass it. This validator therefore enforces two additional checks, making the token usable only if it was
 * minted specifically for us, by our own service account:</p>
 * <ul>
 *   <li><b>Audience</b> - token must target our concrete endpoint URL (audience is set explicitly when Cloud Tasks
 *   tasks are created in <code>GcpTaskEnqueuer.enqueue()</code>).</li>
 *   <li><b>Identity</b> - token must carry 'email' claim equal to our GCP service account email. Only tokens minted
 *   via IAM for that service account have it.</li>
 * </ul>
 */
@Component
@Slf4j
public class JwtGcpTokenValidator implements OAuth2TokenValidator<Jwt> {
  /** Name of the JWT claim holding the principal's email address. */
  private static final String CLAIM_EMAIL = "email";
  /** Error code returned when validation fails. */
  private static final String ERROR_CODE = "invalid_gcp_token";

  /** Public URL of this service. */
  @Value("${app.gcp.general.url}")
  private String serviceUrl;
  /** Name of the GCP service account (without domain). */
  @Value("${app.gcp.general.service-account}")
  private String serviceAccount;
  /** GCP project id. */
  @Value("${app.gcp.general.project}")
  private String projectId;

  /**
   * Validates given JWT against expected audience and expected service account identity.
   * @param jwt Token to validate.
   * @return Success if all checks passed, otherwise failure with error descriptions.
   */
  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    List<String> failures = new ArrayList<>();

    // Check 1: audience must match the exact endpoint URL of this service.
    String expectedAudience = serviceUrl + GcpConst.EMAIL_SEND_ENDPOINT_PATH;
    if (jwt.getAudience() == null || !jwt.getAudience().contains(expectedAudience)) {
      log.warn("GCP token rejected: audience mismatch. Expected '{}', got '{}'.", expectedAudience, jwt.getAudience());
      failures.add("Token audience does not match this service.");
    }

    // Check 2: token must be minted for our service account (checked via 'email' claim).
    String expectedEmail = serviceAccount + "@" + projectId + ".iam.gserviceaccount.com";
    String email = jwt.getClaimAsString(CLAIM_EMAIL);
    // Note: comparison is case-insensitive - Google normalizes case of service account emails in tokens.
    if (email == null || !email.equalsIgnoreCase(expectedEmail)) {
      log.warn("GCP token rejected: unexpected token identity. Expected '{}', got '{}'.", expectedEmail, email);
      failures.add("Token was not issued for the expected service account.");
    }

    if (!failures.isEmpty()) return OAuth2TokenValidatorResult.failure(failures.stream()
        .map(description -> new OAuth2Error(ERROR_CODE, description, null))
        .toList());

    return OAuth2TokenValidatorResult.success();
  }
}
