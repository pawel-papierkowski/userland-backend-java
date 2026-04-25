package org.portfolio.userland.system.history.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
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
public class SystemHistoryService {
  private final SecurityGeneratorService securityGeneratorService;
  private final SystemHistoryRepository systemHistoryRepository;
  private final ClockService clockService;

  @Transactional
  public void addEvent(EnHistoryWho who, EnHistoryWhat what, String value) {
    LocalDateTime nowAt = clockService.getNowUTC();

    SystemHistory systemHistoryEvent = new SystemHistory();
    systemHistoryEvent.setUuid(securityGeneratorService.uuid());
    systemHistoryEvent.setCreatedAt(nowAt);
    systemHistoryEvent.setWho(who);
    systemHistoryEvent.setWhat(what);
    systemHistoryEvent.setValue(value);
    systemHistoryRepository.save(systemHistoryEvent);
  }
}
