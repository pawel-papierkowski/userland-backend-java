package org.portfolio.userland.system.jwt;

import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.UserCannotBeLockedException;
import org.portfolio.userland.features.user.exceptions.UserMustBeActiveException;
import org.portfolio.userland.system.BaseService;
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
  /** How long before JWT token expires in minutes. */
  @Value("${security.jwt.expiration}")
  private long jwtExpiration;

  /**
   * Generate JWT token based on user data.
   * @param user User data.
   * @return Generated JWT token.
   */
  public String generateToken(User user) {
    verifyUser(user);

    LocalDateTime issuedAt = clockService.getNowUTC();
    LocalDateTime expiresAt = issuedAt.plusMinutes(jwtExpiration);
    Date issueDate = clockService.convert(issuedAt);
    Date expirationDate = clockService.convert(expiresAt);

    return Jwts.builder()
        .claims(resolveClaims(user))
        .subject(user.getEmail())
        .issuedAt(issueDate)
        .expiration(expirationDate)
        .signWith(resolveSigningKey())
        .compact();
  }

  /**
   * Checks if user is in state that allows login. Invalid state causes exception.
   * @param user User data.
   */
  private void verifyUser(User user) {
    if (!EnUserStatus.ACTIVE.equals(user.getStatus())) throw new UserMustBeActiveException(user.getEmail());
    if (user.getLocked()) throw new UserCannotBeLockedException(user.getEmail());
  }

  /**
   * Convert user permissions to claims.
   * @param user User data.
   * @return Claims as <code>Map</code>.
   */
  private Map<String, ?> resolveClaims(User user) {
    Map<String, String> claimMap = Maps.newHashMap();

    for (UserPermission permissionEntry : user.getPermissions()) {
      Permission permission = permissionEntry.getPermission();
      if (!permission.getInJwt()) continue;
      claimMap.put(permission.getName(), permissionEntry.getValue());
    }
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
   * Checks if the token is valid by verifying the username matches, and it's not expired.
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
    return Jwts.parser()
        .verifyWith(resolveSigningKey())
        .clock(jwtClock) // ensures we use clockService so tests work correctly when setting arbitrary time
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Generic method to extract a specific claim from the token payload.
   * @param token JWT token.
   * @param claimsResolver Claim to get.
   * @return Value from claimsResolver.
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
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
