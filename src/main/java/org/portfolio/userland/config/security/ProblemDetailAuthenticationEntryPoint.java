package org.portfolio.userland.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Ensure that authentication errors will be handled by <code>GlobalExceptionHandler</code> so that error will return as
 * proper problem detail. Use it in <code>SecurityConfig</code>.
 */
@Service
@RequiredArgsConstructor
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {
  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
    handlerExceptionResolver.resolveException(request, response, null, authException);
  }
}
