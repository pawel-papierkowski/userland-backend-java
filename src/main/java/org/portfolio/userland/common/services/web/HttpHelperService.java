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
   * <p><b>Trust model:</b> we run exclusively on Cloud Run behind the Google Front End (GFE). GFE <i>appends</i>
   * the real connecting client's IP at the end of any client-supplied <code>X-Forwarded-For</code> value, therefore
   * the last entry is the only one we can trust. Everything before it (including the first entry!) is fully
   * attacker-controlled and must be ignored.</p>
   * <p><b>Warning:</b> result of this method is informational only (user history audit records) and must never be
   * used for security decisions (rate limiting, bans, lockouts) without revisiting this trust model first.</p>
   * @return Ip.
   */
  private String resolveClientIp() {
    String ip = request.getHeader("X-Forwarded-For");
    // If the header is missing, fall back to the direct remote address.
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
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
