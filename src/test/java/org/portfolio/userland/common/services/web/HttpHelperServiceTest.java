package org.portfolio.userland.common.services.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link HttpHelperService}.
 */
public class HttpHelperServiceTest {

  // //////////////////////////////////////////////////////////////////////////
  // resolveHttpParams

  @Test
  public void resolveHttpParamsWithHeaders() {
    // Arrange
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.50");
    when(mockRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    HttpHelperService httpHelperService = new HttpHelperService(mockRequest);

    // Act
    String result = httpHelperService.resolveHttpParams();

    // Assert
    assertThat(result).isEqualTo("IP: '192.168.1.50', User-Agent: 'Mozilla/5.0'");
  }

  @Test
  public void resolveHttpParamsFallbackToRemoteAddr() {
    // Arrange
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(mockRequest.getHeader("User-Agent")).thenReturn("curl/7.68");
    when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.1");
    HttpHelperService httpHelperService = new HttpHelperService(mockRequest);

    // Act
    String result = httpHelperService.resolveHttpParams();

    // Assert
    assertThat(result).isEqualTo("IP: '10.0.0.1', User-Agent: 'curl/7.68'");
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveClientIp (tested via resolveHttpParams)

  @Test
  public void resolveClientIpFromXffSingleEntry() {
    // Arrange
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.50");
    when(mockRequest.getHeader("User-Agent")).thenReturn("test");
    when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    HttpHelperService httpHelperService = new HttpHelperService(mockRequest);

    // Act
    String result = httpHelperService.resolveHttpParams();

    // Assert
    assertThat(result).contains("IP: '192.168.1.50'");
  }

  @Test
  public void resolveClientIpFromXffMultipleEntries() {
    // Arrange: first entry is attacker-controlled, last entry is trusted (appended by GFE)
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("6.6.6.6, 192.168.1.50");
    when(mockRequest.getHeader("User-Agent")).thenReturn("test");
    when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    HttpHelperService httpHelperService = new HttpHelperService(mockRequest);

    // Act
    String result = httpHelperService.resolveHttpParams();

    // Assert
    assertThat(result).contains("IP: '192.168.1.50'");
  }

  @Test
  public void resolveClientIpFallbackWhenNoXff() {
    // Arrange
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    when(mockRequest.getHeader("User-Agent")).thenReturn("test");
    when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.1");
    HttpHelperService httpHelperService = new HttpHelperService(mockRequest);

    // Act
    String result = httpHelperService.resolveHttpParams();

    // Assert
    assertThat(result).contains("IP: '10.0.0.1'");
  }

  @Test
  public void resolveClientIpFallbackWhenXffUnknown() {
    // Arrange
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("unknown");
    when(mockRequest.getHeader("User-Agent")).thenReturn("test");
    when(mockRequest.getRemoteAddr()).thenReturn("10.0.0.1");
    HttpHelperService httpHelperService = new HttpHelperService(mockRequest);

    // Act
    String result = httpHelperService.resolveHttpParams();

    // Assert
    assertThat(result).contains("IP: '10.0.0.1'");
  }
}
