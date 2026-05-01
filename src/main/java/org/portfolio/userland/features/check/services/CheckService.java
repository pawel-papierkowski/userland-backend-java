package org.portfolio.userland.features.check.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.check.data.CheckInfoResp;
import org.portfolio.userland.system.BaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for check endpoints.
 */
@Service
@RequiredArgsConstructor
public class CheckService extends BaseService {
  private final ApplicationContext context;

  /** System name. */
  @Value("${app.main.name}")
  protected String name;
  /** System version. */
  @Value("${app.main.version}")
  protected String version;

  /**
   * Resolve basic information about system.
   * @return Various basic information.
   */
  public CheckInfoResp info() {
    LocalDateTime bootAt = clockService.convert(context.getStartupDate());
    return CheckInfoResp.builder()
        .name(name)
        .nowAt(clockService.getNowUTC())
        .bootAt(bootAt)
        .version(version)
        .profile(profile)
        .build();
  }
}
