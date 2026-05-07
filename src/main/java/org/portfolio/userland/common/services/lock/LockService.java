package org.portfolio.userland.common.services.lock;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.portfolio.userland.common.services.clock.ClockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Lock service. Leverages ShedLock.
 */
@Service
@RequiredArgsConstructor
public class LockService {
  private final ClockService clockService;
  private final LockProvider lockProvider;

  /**
   * Attempt running a task with a ShedLock lock.
   * @param lockName Name of lock. Must match the name used in your @SchedulerLock annotation!
   * @param task Task to run.
   * @return True if executed successfully. False if failed to acquire lock.
   */
  public boolean runWithLock(String lockName, Runnable task) {
    // Define the lock configuration.
    LockConfiguration lockConfig = new LockConfiguration(
        clockService.getInstant(),
        lockName,
        Duration.ofMinutes(15),
        Duration.ZERO
    );

    // Attempt to acquire the lock.
    Optional<SimpleLock> lock = lockProvider.lock(lockConfig);

    // The lock is currently held by something else (the scheduler or another manual call).
    if (lock.isEmpty()) return false;

    try {
      // Lock acquired successfully, run the task.
      task.run();
      return true;
    } finally {
      // Always release the lock when the manual run is complete.
      lock.get().unlock();
    }
  }

  /**
   * Attempt running a task with a ShedLock lock. This method is designed to be run in Controller endpoint.
   * @param lockName Name of lock. Must match the name used in your @SchedulerLock annotation!
   * @param task Task to run.
   * @return Response for HTTP call.
   */
  public ResponseEntity<Void> endpointWithLock(String lockName, Runnable task) {
    boolean result = runWithLock(lockName, task);

    if (result) return new ResponseEntity<>(HttpStatus.OK);
    // The lock is currently held by something else (the scheduler, another manual call).
    return new ResponseEntity<>(HttpStatus.LOCKED); // HTTP 423
  }
}
