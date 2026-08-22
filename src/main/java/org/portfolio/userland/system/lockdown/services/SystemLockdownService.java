package org.portfolio.userland.system.lockdown.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.user.repositories.jwt.UserJwtRepository;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.portfolio.userland.system.base.BaseService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.history.entities.EnHistoryWhat;
import org.portfolio.userland.system.history.entities.EnHistoryWho;
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
  @Transactional(readOnly = true)
  public SystemLockdownResp get() {
    String lockdownValue = configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF);
    return SystemLockdownResp.builder().state(EnSystemLockdownState.fromStr(lockdownValue)).build();
  }

  /**
   * Set new state of system lockdown.
   * <p>Note: the decision whether the state changes at all is made atomically together with write itself
   * (single conditional UPDATE on the config row), so concurrent toggles cannot both observe stale state and
   * perform side effects twice - exactly one of them will perform the change.</p>
   * @param systemLockdownReq New state of system lockdown.
   * @return True if lockdown state was changed, otherwise false.
   */
  @Transactional
  public boolean set(SystemLockdownReq systemLockdownReq) {
    EnSystemLockdownState newLockDownState = systemLockdownReq.state();
    if (newLockDownState == null) return false;

    String newValue = switch (newLockDownState) {
      case ON -> ConfigConst.TRUE;
      case OFF -> ConfigConst.FALSE;
    };

    // Atomically decide whether anything changes at all. Zero rows means the state was already equal (or the config
    // variable is missing, in which case ConfigUnknownException is thrown by setIfChanged).
    int updated = configService.setIfChanged(ConfigConst.USER_LOCKDOWN, newValue);
    if (updated == 0) return false;

    switch (newLockDownState) {
      case ON -> lockSystem();
      case OFF -> unlockSystem();
    }
    return true;
  }

  /**
   * Activate system lockdown. Note the lockdown config variable was already set to ON by {@link #set}.
   */
  private void lockSystem() {
    // Revoke all JWTs except ones belonging to admin users. Result is that all users (except admin) have their sessions
    // invalidated, effectively kicking them out of system. They also cannot call any endpoint, even those that normally
    // work without user logged in.
    Map<String, Set<String>> allowedPermissions = permissionService.getMap(EnPermKind.ACCESS_TO_ADMIN_PANEL);
    userJwtRepository.revokeAllTokensExcept(allowedPermissions);

    systemHistoryService.addEvent(EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");
    log.warn("SYSTEM LOCKDOWN ACTIVATED.");
  }

  /**
   * Deactivate system lockdown. Note the lockdown config variable was already set to OFF by {@link #set}.
   */
  private void unlockSystem() {
    systemHistoryService.addEvent(EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "OFF");
    log.info("SYSTEM LOCKDOWN DEACTIVATED.");
  }
}
