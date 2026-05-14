package org.portfolio.userland.common.services.web;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.constants.UserConst;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Helper methods involving web, including frontend-related things.
 */
@Service
@RequiredArgsConstructor
public class WebHelperService {
  /** Base frontend address. */
  @Value("${app.main.www}")
  private String frontendWww;


  /**
   * Resolve login link. Note it is for frontend, not backend.
   * @param frontend Name of used frontend.
   * @return Login link.
   */
  public String resolveLoginLink(EnFrontendFramework frontend) {
    // Note it is linking to frontend - actual backend login endpoint will be called by frontend.
    return resolveWww(frontend) + "/login";
  }

  /**
   * Resolve WWW address of frontend. It consists of base www address (frontendWww) and suffix indicating what frontend
   * framework was used.
   * @param frontend Used frontend framework.
   * @return WWW address of frontend. Example: https://pawelpapierkowski.net.pl/userland-frontend-vue
   */
  public String resolveWww(EnFrontendFramework frontend) {
    String suffix = frontend == null ? UserConst.FRONTEND_DEF.name().toLowerCase() : frontend.name().toLowerCase();
    return frontendWww + suffix;
  }
}
