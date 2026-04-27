package org.portfolio.userland.system.history.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserDoesNotExistException;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.system.BaseService;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.data.CustomUserDetails;
import org.portfolio.userland.system.history.entity.EnHistoryWhat;
import org.portfolio.userland.system.history.entity.EnHistoryWho;
import org.portfolio.userland.system.history.entity.SystemHistory;
import org.portfolio.userland.system.history.repositories.SystemHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for system history.
 */
@Service
@RequiredArgsConstructor
public class SystemHistoryService extends BaseService {
  private final SecurityGeneratorService securityGeneratorService;
  private final SystemHistoryRepository systemHistoryRepository;
  private final UserRepository userRepository;

  /**
   * Add system history event. Code will attempt to resolve currently logged user beforehand.
   * @param who Who did that?
   * @param what What happened?
   * @param value Value.
   */
  @Transactional
  public void addEvent(EnHistoryWho who, EnHistoryWhat what, String value) {
    // Try to resolve logged user, if any exists.
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    User user = null;
    if (userDetails != null) {
      user = userRepository.findByEmail(userDetails.getEmail())
          .orElseThrow(() -> new UserDoesNotExistException(userDetails.getEmail()));
    }
    addEvent(user, who, what, value);
  }

  /**
   * Add system history event.
   * @param user User. Can be null.
   * @param who Who did that?
   * @param what What happened?
   * @param value Value.
   */
  @Transactional
  public void addEvent(User user, EnHistoryWho who, EnHistoryWhat what, String value) {
    LocalDateTime nowAt = clockService.getNowUTC();

    SystemHistory systemHistoryEvent = new SystemHistory();
    systemHistoryEvent.setUuid(securityGeneratorService.uuid());
    systemHistoryEvent.setCreatedAt(nowAt);
    systemHistoryEvent.setUser(user);
    systemHistoryEvent.setWho(who);
    systemHistoryEvent.setWhat(what);
    systemHistoryEvent.setValue(value);
    systemHistoryRepository.save(systemHistoryEvent);
  }
}
