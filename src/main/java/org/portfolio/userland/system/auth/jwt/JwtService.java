package org.portfolio.userland.system.auth.jwt;

import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles JWT tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService extends BaseService {
  private final JwtClock jwtClock;

  /** Secret key used to sign JWT tokens. Must be at least 256 bits (32 characters) long. */
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
   */
  @PostConstruct
  private void initSigningAssets() {
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
   *   <li>perm - permissions</li>
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
   * Resolves signing key.
   * @return Secret key.
   */
  private SecretKey resolveSigningKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  //

  /**
   * Checks if the token is valid by verifying the email matches, and it's not expired.
   * @param token JWT token.
   * @param email Email.
   * @return True if token is valid, otherwise false.
   */
  public boolean isTokenValid(String token, String email) {
    try {
      final String emailInToken = extractEmail(token);
      return (emailInToken.equals(email)) && !isTokenExpired(token);
    } catch (JwtException | IllegalArgumentException ex) {
      // Token is malformed, expired, or signature is invalid.
      return false;
    }
  }

  /**
   * Checks if JWT token is expired.
   * @param token JWT token.
   * @return True if token is expired, otherwise false.
   */
  private boolean isTokenExpired(String token) {
    LocalDateTime nowAt = clockService.getNowUTC();
    Date nowDate = Date.from(nowAt.atZone(ZoneId.systemDefault()).toInstant());
    Date extractedDate = extractClaim(token, Claims::getExpiration);
    return extractedDate.before(nowDate);
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

  /**
   * Generic method to extract a specific claim from the token payload.
   * @param token JWT token.
   * @param claimsResolver Claim to get.
   * @return Value from claimsResolver.
   */
  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Extract email claim from the token payload.
   * @param token JWT token.
   * @return Email.
   */
  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }
}
