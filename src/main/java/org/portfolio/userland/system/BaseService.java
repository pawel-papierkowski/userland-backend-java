package org.portfolio.userland.system;

import org.portfolio.userland.common.constants.EnAppProfile;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.system.auth.PermissionService;
import org.portfolio.userland.system.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Base for all standard services.
 */
public abstract class BaseService {
  @Autowired
  protected SecurityGeneratorService securityGeneratorService;
  @Autowired
  protected ApplicationEventPublisher eventPublisher;

  @Autowired
  protected ConfigService configService;
  @Autowired
  protected PermissionService permissionService;

  @Autowired
  protected ClockService clockService;

  /** System profile. */
  @Value("${app.main.profile}")
  protected EnAppProfile profile;
}
