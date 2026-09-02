package org.portfolio.userland.system.auth.jwt;

import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.exception.SystemMisconfigurationException;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserInvalidStatusException;
import org.portfolio.userland.features.user.exceptions.UserLockedException;
import org.portfolio.userland.system.auth.jwt.constants.JwtClaims;
import org.portfolio.userland.system.auth.perm.PermissionHelper;
import org.portfolio.userland.system.base.BaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Handles JWT tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService extends BaseService {
  /**
   * Placeholder value committed in configuration for local property resolution. It is publicly known,
   * so it must never reach the signing key - otherwise anyone could forge valid tokens.
   */
  private static final String PLACEHOLDER_SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

  /** Minimal length of decoded secret key bytes (256 bits), required by HS256. */
  private static final int MIN_SECRET_KEY_BYTES = 32;

  private final JwtClock jwtClock;

  /** Secret key used to sign JWT tokens. Must be at least 256 bits (32 bytes) long. Note it is in base64. */
  @Value("${security.jwt.secret}")
  private String secretKey;

  /** Precomputed signing key, built once from the secret in {@link #initSigningAssets()} since it never changes. */
  private SecretKey signingKey;

  /** Precomputed JWT parser, built once from the signing key and clock in {@link #initSigningAssets()}. Thread-safe and reusable. */
  private JwtParser jwtParser;

  /**
   * Builds the signing key and parser once at startup. The secret is static config,
   * so both are immutable for the lifetime of the application; rebuilding them per operation
   * would only waste CPU on every token generation and parse.
   * <p>Fails fast on invalid secrets (missing, committed placeholder, too short or malformed),
   * so a misconfigured deployment refuses to boot instead of silently signing tokens
   * with a publicly known key.</p>
   */
  @PostConstruct
  private void initSigningAssets() {
    verifySecret(secretKey);
    signingKey = resolveSigningKey();
    jwtParser = Jwts.parser()
        .verifyWith(signingKey)
        .clock(jwtClock) // ensures we use clockService so tests work correctly when setting arbitrary time
        .build();
  }

  /**
   * Generate JWT token based on user data.
   * @param user User data.
   * @return Generated JWT token.
   */
  public String generateToken(User user) {
    return generateToken(user, null);
  }

  /**
   * Generate JWT token based on user data.
   * @param user User data.
   * @param customExpiration Custom expiration period in minutes. Can be null, will use default expiration.
   * @return Generated JWT token.
   */
  public String generateToken(User user, Long customExpiration) {
    verifyUser(user);

    LocalDateTime issuedAt = clockService.getNowUTC();
    LocalDateTime expiresAt = userHelperService.resolveJwtExpiration(issuedAt, customExpiration);
    Date issueDate = clockService.convert(issuedAt);
    Date expirationDate = clockService.convert(expiresAt);

    return Jwts.builder()
        .claims(resolveClaims(user))
        .subject(user.getEmail())
        .issuedAt(issueDate)
        .expiration(expirationDate)
        .signWith(signingKey)
        .compact();
  }

  /**
   * Checks if user is in state that allows login. Invalid state causes exception.
   * @param user User data.
   */
  private void verifyUser(User user) {
    if (!EnUserStatus.ACTIVE.equals(user.getStatus())) throw new UserInvalidStatusException(user.getEmail());
    if (user.getLocked()) throw new UserLockedException(user.getEmail());
  }

  /**
   * Convert user data to claims. Custom claims:
   * <ul>
   *   <li>name - name of user</li>
   *   <li>perms - permissions</li>
   * </ul>
   * @param user User data.
   * @return Claims as <code>Map</code>.
   */
  private Map<String, ?> resolveClaims(User user) {
    Map<String, String> permMap = Maps.newHashMap();
    permMap.putAll(PermissionHelper.resolvePermissions(user));

    Map<String, Object> claimMap = Maps.newHashMap();
    claimMap.put(JwtClaims.NAME, user.getUsername());
    claimMap.put(JwtClaims.PERMS, permMap);
    return claimMap;
  }

  /**
   * Verifies that secret is safe to use for signing. Guards against the case where <code>JWT_SECRET</code>
   * environment variable is unset in a deployment and configuration falls back to the committed placeholder -
   * that key is publicly known, so tokens signed with it could be forged by anyone.
   * @param secret Raw BASE64-encoded secret from configuration.
   */
  private void verifySecret(String secret) {
    if (StringUtils.isBlank(secret)) throw new SystemMisconfigurationException("JWT secret is not present! Set the JWT_SECRET environment variable.");
    if (PLACEHOLDER_SECRET.equals(secret)) throw new SystemMisconfigurationException("JWT secret is set to publicly known placeholder! Set the JWT_SECRET environment variable.");
  }

  /**
   * Resolves signing key.
   * @return Secret key.
   */
  private SecretKey resolveSigningKey() {
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(secretKey);
    } catch (DecodingException ex) {
      throw new SystemMisconfigurationException("JWT secret is not valid BASE64.");
    }
    if (keyBytes.length < MIN_SECRET_KEY_BYTES) throw new SystemMisconfigurationException("JWT secret must be at least 256 bits (32 bytes) long.");
    return Keys.hmacShaKeyFor(keyBytes);
  }

  //

  /**
   * Parses the token and returns all claims.
   * If the token is invalid or expired, this will throw a JwtException.
   * @param token JWT token.
   * @return All claims.
   */
  public Claims extractAllClaims(String token) {
    return jwtParser
        .parseSignedClaims(token)
        .getPayload();
  }
}
