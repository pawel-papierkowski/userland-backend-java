package org.portfolio.userland.system;

import org.portfolio.userland.common.constants.EnAppProfile;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.services.UserHelperService;
import org.portfolio.userland.system.auth.perm.PermissionService;
import org.portfolio.userland.system.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

/**
 * <p>Base for all standard services. No actual executable code should be here, just fields for stuff that is used
 * literally everywhere as convenience. That prevents "God Class".</p>
 * <p>From this class domain-specific base service classes are inherited. For example, <code>BaseService</code> ->
 * <code>BaseUserService</code> -> <code>UserRegisterService</code>, <code>UserLoginService</code>,
 * <code>UserPasswordService</code> etc.</p>
 * <p>Note: not all services implement base service, only those that need it.</p>
 */
public abstract class BaseService {
  /** System configuration. */
  @Autowired
  protected ConfigService configService;
  /** Permission checking. */
  @Autowired
  protected PermissionService permissionService;
  /** Helper for basic user handling. */
  @Autowired
  protected UserHelperService userHelperService;

  /** Date & time. */
  @Autowired
  protected ClockService clockService;
  /** Generator of random tokens, UUIDs etc. */
  @Autowired
  protected SecurityGeneratorService securityGeneratorService;

  /** Spring events. */
  @Autowired
  protected ApplicationEventPublisher eventPublisher;

  /** System profile. */
  @Value("${app.main.profile}")
  protected EnAppProfile profile;
}
