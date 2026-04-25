package org.portfolio.userland.test.helpers.factories;

import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Common class for all factories.
 */
public class BaseFactory {
  @Autowired
  protected SecurityGeneratorService securityGeneratorService;
  @Autowired
  protected ClockService clockService;
}
