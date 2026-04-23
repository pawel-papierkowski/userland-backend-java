package org.portfolio.userland.test.helpers.context;

import org.portfolio.userland.system.auth.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Security context factory for mocking <code>CustomUserDetails</code>.
 */
public class WithMockCustomUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockCustomUser> {

  @Override
  public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    List<SimpleGrantedAuthority> authorities = Arrays.stream(annotation.authorities())
        .map(SimpleGrantedAuthority::new)
        .toList();

    CustomUserDetails principal = buildPrincipal(annotation, authorities);
    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,
            principal.getPassword(),
            principal.getAuthorities()
        );

    context.setAuthentication(authentication);
    return context;
  }

  /**
   * Creates principal based on annotation data.
   * @param annotation Annotation.
   * @param authorities Authorities.
   * @return Created principal.
   */
  private CustomUserDetails buildPrincipal(WithMockCustomUser annotation, List<SimpleGrantedAuthority> authorities) {
    return new CustomUserDetails(
        annotation.id(),
        annotation.active(),
        annotation.locked(),
        annotation.username(),
        annotation.email(),
        annotation.password(),
        new HashSet<>(Arrays.stream(annotation.jwts()).toList()),
        authorities
    );
  }
}
