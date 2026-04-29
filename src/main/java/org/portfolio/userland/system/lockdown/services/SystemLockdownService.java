package org.portfolio.userland.system.lockdown.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.user.repositories.UserJwtRepository;
import org.portfolio.userland.system.BaseService;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.history.entity.EnHistoryWhat;
import org.portfolio.userland.system.history.entity.EnHistoryWho;
import org.portfolio.userland.system.history.services.SystemHistoryService;
import org.portfolio.userland.system.lockdown.dto.EnSystemLockdownState;
import org.portfolio.userland.system.lockdown.dto.SystemLockdownReq;
import org.portfolio.userland.system.lockdown.dto.SystemLockdownResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Handles system lockdown.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLockdownService extends BaseService {
  private final SystemHistoryService systemHistoryService;

  private final UserJwtRepository userJwtRepository;

  /**
   * Retrieve state of system lockdown.
   * @return State of system lockdown.
   */
  @Transactional
  public SystemLockdownResp get() {
    String lockdownValue = configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF);
    return new SystemLockdownResp(EnSystemLockdownState.fromStr(lockdownValue));
  }

  /**
   * Set new state of system lockdown.
   * @param systemLockdownReq New state of system lockdown.
   * @return True if lockdown state was changed, otherwise false.
   */
  @Transactional
  public boolean set(@Valid SystemLockdownReq systemLockdownReq) {
    String lockdownValue = configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF);
    EnSystemLockdownState currLockDownState = EnSystemLockdownState.fromStr(lockdownValue);
    EnSystemLockdownState newLockDownState = systemLockdownReq.state();

    // nothing changes
    if (currLockDownState == null || newLockDownState == null || currLockDownState.equals(newLockDownState)) return false;

    switch (newLockDownState) {
      case ON -> lockSystem();
      case OFF -> unlockSystem();
    }
    return true;
  }

  /** Activate system lockdown. */
  private void lockSystem() {
    configService.set(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);
    // revoke all user.jwt except ones from admin users
    Map<String, Set<String>> allowedPermissions = permissionService.get(EnPermKind.ACCESS_TO_ADMIN_PANEL);
    userJwtRepository.revokeAllTokensExcept(allowedPermissions);

    systemHistoryService.addEvent(EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");
    log.warn("SYSTEM LOCKDOWN ACTIVATED.");
  }

  /** Deactivate system lockdown. */
  private void unlockSystem() {
    configService.set(ConfigConst.USER_LOCKDOWN, ConfigConst.FALSE);

    systemHistoryService.addEvent(EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "OFF");
    log.info("SYSTEM LOCKDOWN DEACTIVATED.");
  }
}
