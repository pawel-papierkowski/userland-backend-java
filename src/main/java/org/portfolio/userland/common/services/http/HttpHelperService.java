package org.portfolio.userland.common.services.http;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Helper methods involving HTTP request.
 */
@Service
@RequiredArgsConstructor
public class HttpHelperService {
  /** HTTP request (meta)data. */
  private final HttpServletRequest request;

  /**
   * Resolve HTTP request params for event in user history.
   * @return Params.
   */
  public String resolveHttpParams() {
    String params = "";
    params += "IP: '"+resolveClientIp()+"'";
    params += ", User-Agent: '"+request.getHeader("User-Agent")+"'";
    return params;
  }

  /**
   * Resolve client ip. Needed in case we run behind a proxy, load balancer, or Docker network. In this case
   * <code>request.getRemoteAddr()</code> will return useless or wrong ip.
   * @return Ip.
   */
  private String resolveClientIp() {
    String ip = request.getHeader("X-Forwarded-For");
    // If the header is missing, fall back to the direct remote address.
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
    else {
      // X-Forwarded-For can contain multiple IPs if it passed through multiple proxies.
      // The first IP is always the original client.
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}
