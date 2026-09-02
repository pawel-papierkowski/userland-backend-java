package org.portfolio.userland.common.services.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Helper methods involving HTTP requests.
 */
@Service
@RequiredArgsConstructor
public class HttpHelperService {
  /** HTTP request (meta)data. */
  private final HttpServletRequest request;

  /**
   * Resolve HTTP request params. Useful for logging, history events etc.
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
   * <p><b>Trust model:</b> we run exclusively on Cloud Run behind the Google Front End (GFE). GFE <i>appends</i>
   * the real connecting client's IP at the end of any client-supplied <code>X-Forwarded-For</code> value, therefore
   * the last entry is the only one we can trust. Everything before it (including the first entry!) is fully
   * attacker-controlled and must be ignored.</p>
   * <p><b>Note:</b> only last entry can be trusted and used for security decisions (rate limiting, bans, lockouts).
   * This is what we return.</p>
   * @return Ip.
   */
  public String resolveClientIp() {
    return resolveClientIp(request);
  }

  /**
   * Resolve client ip from the given request. Use this overload when the {@link HttpServletRequest} is available
   * directly (e.g. in filters), to avoid relying on the request-scoped proxy.
   * @param req The HTTP request to resolve the client IP from.
   * @return Ip.
   * @see #resolveClientIp()
   */
  public String resolveClientIp(HttpServletRequest req) {
    String ip = req.getHeader("X-Forwarded-For");
    // If the header is missing, fall back to the direct remote address.
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = req.getRemoteAddr();
    else {
      // X-Forwarded-For can contain multiple IPs if it passed through multiple proxies.
      // The last IP was appended by our trusted front end (GFE) and represents the real connecting address.
      // Any earlier entries were supplied by the client and cannot be trusted.
      String[] ips = ip.split(",");
      ip = ips[ips.length - 1].trim();
    }
    return ip;
  }
}
