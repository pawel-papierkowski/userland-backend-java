package org.portfolio.userland.common.services.lock;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link LockService}.
 */
public class LockServiceTest extends BaseIntegrationTest {
  @Autowired
  private LockService lockService;

  // //////////////////////////////////////////////////////////////////////////
  // runWithLock

  @Test
  public void runWithLockExecutesTask() throws Exception {
    // Arrange
    String lockName = UUID.randomUUID().toString();
    AtomicBoolean taskRan = new AtomicBoolean(false);

    // Act
    boolean result = lockService.runWithLock(lockName, () -> taskRan.set(true));

    // Assert
    assertThat(result).as("Should return true when lock is acquired").isTrue();
    assertThat(taskRan).as("Task should have been executed").isTrue();
  }

  @Test
  public void runWithLockReturnsFalseWhenLocked() throws Exception {
    // Arrange
    String lockName = UUID.randomUUID().toString();
    CountDownLatch lockHeld = new CountDownLatch(1);
    CountDownLatch attemptDone = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    // Act: Thread 1 acquires the lock and holds it.
    Future<?> holder = executor.submit(() -> {
      boolean acquired = lockService.runWithLock(lockName, () -> {
        lockHeld.countDown(); // signal that lock is held
        try {
          Thread.sleep(3000); // hold the lock
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
      assertThat(acquired).isTrue();
    });

    // Wait until Thread 1 holds the lock.
    lockHeld.await(5, TimeUnit.SECONDS);

    // Thread 2 attempts to acquire the same lock.
    Future<Boolean> contender = executor.submit(() -> lockService.runWithLock(lockName, () -> {
    }));
    boolean contenderResult = contender.get(5, TimeUnit.SECONDS);

    // Assert
    assertThat(contenderResult).as("Should return false when lock is already held").isFalse();

    // Cleanup
    holder.get(5, TimeUnit.SECONDS);
    executor.shutdownNow();
  }

  @Test
  public void runWithLockReleasesLockAfterExecution() throws Exception {
    // Arrange
    String lockName = UUID.randomUUID().toString();

    // Act: First call acquires and releases.
    boolean first = lockService.runWithLock(lockName, () -> {});
    // Second call should succeed since lock was released.
    boolean second = lockService.runWithLock(lockName, () -> {});

    // Assert
    assertThat(first).as("First call should succeed").isTrue();
    assertThat(second).as("Second call should succeed after lock release").isTrue();
  }

  @Test
  public void runWithLockReleasesLockOnException() throws Exception {
    // Arrange
    String lockName = UUID.randomUUID().toString();

    // Act & Assert: First call throws, but should release lock.
    assertThatThrownBy(() -> lockService.runWithLock(lockName, () -> {
      throw new RuntimeException("task failed");
    })).isInstanceOf(RuntimeException.class).hasMessage("task failed");

    // Second call should succeed since lock was released in finally block.
    boolean second = lockService.runWithLock(lockName, () -> {});
    assertThat(second).as("Second call should succeed after exception released lock").isTrue();
  }

  // //////////////////////////////////////////////////////////////////////////
  // endpointWithLock

  @Test
  public void endpointWithLockReturns204() {
    // Arrange
    String lockName = UUID.randomUUID().toString();

    // Act
    ResponseEntity<Void> response = lockService.endpointWithLock(lockName, () -> {});

    // Assert
    assertThat(response.getStatusCode()).as("Should return 204 NO_CONTENT").isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  public void endpointWithLockReturns423WhenLocked() throws Exception {
    // Arrange
    String lockName = UUID.randomUUID().toString();
    CountDownLatch lockHeld = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    // Act: Thread 1 acquires the lock and holds it.
    Future<?> holder = executor.submit(() -> {
      boolean acquired = lockService.runWithLock(lockName, () -> {
        lockHeld.countDown();
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
      assertThat(acquired).isTrue();
    });

    lockHeld.await(5, TimeUnit.SECONDS);

    // Thread 2 attempts via endpointWithLock.
    Future<ResponseEntity<Void>> contender = executor.submit(() -> lockService.endpointWithLock(lockName, () -> {}));
    ResponseEntity<Void> response = contender.get(5, TimeUnit.SECONDS);

    // Assert
    assertThat(response.getStatusCode()).as("Should return 423 LOCKED").isEqualTo(HttpStatus.LOCKED);

    // Cleanup
    holder.get(5, TimeUnit.SECONDS);
    executor.shutdownNow();
  }
}
