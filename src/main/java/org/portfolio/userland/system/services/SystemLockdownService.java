package org.portfolio.userland.system.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.repositories.UserJwtRepository;
import org.portfolio.userland.system.auth.PermissionService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.system.dto.lockdown.EnSystemLockdownState;
import org.portfolio.userland.system.dto.lockdown.SystemLockdownReq;
import org.portfolio.userland.system.dto.lockdown.SystemLockdownResp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Handles system lockdown.
 */
@Service
@RequiredArgsConstructor
public class SystemLockdownService {
  private final PermissionService permissionService;
  private final ConfigService configService;
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
   */
  @Transactional
  public void set(@Valid SystemLockdownReq systemLockdownReq) {
    String lockdownValue = configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF);
    EnSystemLockdownState currLockDownState = EnSystemLockdownState.fromStr(lockdownValue);
    EnSystemLockdownState newLockDownState = systemLockdownReq.state();

    // nothing changes
    if (currLockDownState == null || newLockDownState == null || currLockDownState.equals(newLockDownState)) return;

    switch (newLockDownState) {
      case ON -> lockSystem();
      case OFF -> unlockSystem();
    }
  }

  /** Activate system lockdown. */
  private void lockSystem() {
    configService.set(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);
    // revoke all user.jwt except ones from admin users
    Map<String, List<String>> allowedPermissions = permissionService.hasAccessToAdminPanelMap();
    userJwtRepository.revokeAllTokensExcept(allowedPermissions);
  }

  /** Deactivate system lockdown. */
  private void unlockSystem() {
    configService.set(ConfigConst.USER_LOCKDOWN, ConfigConst.FALSE);
  }
}
