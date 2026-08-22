package org.portfolio.userland.system.lockdown;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.BaseSystemTest;
import org.portfolio.userland.system.config.repositories.ConfigRepository;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.history.entities.EnHistoryWhat;
import org.portfolio.userland.system.history.entities.SystemHistory;
import org.portfolio.userland.system.history.repositories.SystemHistoryRepository;
import org.portfolio.userland.system.lockdown.dto.EnSystemLockdownState;
import org.portfolio.userland.system.lockdown.dto.SystemLockdownReq;
import org.portfolio.userland.system.lockdown.services.SystemLockdownService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that concurrent toggling of system lockdown is safe - exactly one request must perform the change (and its
 * side effects like JWT revocation and history event), all others must be reported as no-op.
 */
public class SystemLockdownRaceTest extends BaseSystemTest {
  /** Service under test. Called directly (not via MockMvc), so each call runs in its own transaction. */
  @Autowired
  private SystemLockdownService systemLockdownService;

  @Autowired
  private ConfigRepository configRepository;
  @Autowired
  private SystemHistoryRepository systemHistoryRepository;
  @Autowired
  private TransactionTemplate transactionTemplate;

  /**
   * Reset state of database so test does not interfere with other tests (or its own previous runs).
   */
  @BeforeEach
  protected void resetDb() {
    resetDatabase();
  }

  //
  // Note: history events are attributed via AuthHelper (SecurityContext), which is not set when calling the service
  // directly - this is fine for these tests, we assert only on presence/count of events, not their attribution.
  //

  /**
   * Verifies that two concurrent activations of lockdown result in exactly one performed change: config is ON and
   * exactly one LOCKDOWN history event exists (the second request must be a clean no-op).
   */
  @Test
  public void onlyOneConcurrentActivationWins() throws Exception {
    // Arrange: Lockdown is OFF by default. Both requests want to activate it.
    SystemLockdownReq req = new SystemLockdownReq(EnSystemLockdownState.ON);

    List<String> outcomes = runConcurrently(req, req);

    assertRaceOutcome(outcomes, ConfigConst.TRUE, "ON");
  }

  /**
   * Verifies that two concurrent deactivations of lockdown result in exactly one performed change: config is OFF and
   * exactly one LOCKDOWN history event exists (the second request must be a clean no-op).
   */
  @Test
  public void onlyOneConcurrentDeactivationWins() throws Exception {
    // Arrange: Activate lockdown first.
    transactionTemplate.execute(_ -> {
      configRepository.updateValueByName(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);
      return null;
    });

    // Arrange: Both requests want to deactivate it.
    SystemLockdownReq req = new SystemLockdownReq(EnSystemLockdownState.OFF);

    List<String> outcomes = runConcurrently(req, req);

    assertRaceOutcome(outcomes, ConfigConst.FALSE, "OFF");
  }

  //

  /**
   * Run two lockdown state changes concurrently and collect their outcomes.
   * @param reqA First request.
   * @param reqB Second request.
   * @return Outcome of each call: "CHANGED" if it changed the state, "UNCHANGED" if it was a no-op,
   * or description of unexpected error.
   */
  private List<String> runConcurrently(SystemLockdownReq reqA, SystemLockdownReq reqB) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    Callable<String> taskA = guard(barrier, reqA);
    Callable<String> taskB = guard(barrier, reqB);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    List<String> outcomes = new ArrayList<>();
    try {
      List<Future<String>> results = pool.invokeAll(List.of(taskA, taskB));
      for (Future<String> f : results) {
        try {
          outcomes.add(f.get(10, TimeUnit.SECONDS));
        } catch (ExecutionException ex) {
          outcomes.add("UNEXPECTED: "+ex.getCause());
        }
      }
    } finally {
      pool.shutdownNow();
    }
    return outcomes;
  }

  /**
   * Wrap lockdown change call so it waits on barrier first.
   * @param barrier Barrier to wait on.
   * @param req Request to use.
   * @return Guarded task.
   */
  private Callable<String> guard(CyclicBarrier barrier, SystemLockdownReq req) {
    return () -> {
      barrier.await(5, TimeUnit.SECONDS);
      try {
        return systemLockdownService.set(req) ? "CHANGED" : "UNCHANGED"; // runs in its own transaction
      } finally {
        barrier.await(5, TimeUnit.SECONDS); // ensure both transactions overlap as much as possible
      }
    };
  }

  /**
   * Assert results of a lockdown toggle race: exactly one change was performed, final config value is correct and
   * exactly one LOCKDOWN history event was recorded.
   * @param outcomes Outcomes as returned by {@link #runConcurrently}.
   * @param expectedConfigValue Expected value of lockdown config variable after the race.
   * @param expectedParams Expected params of the single LOCKDOWN history event.
   */
  private void assertRaceOutcome(List<String> outcomes, String expectedConfigValue, String expectedParams) {
    // Assert: Exactly one request changed the state, the other was a clean no-op.
    assertThat(outcomes).as("Exactly one request must perform the change")
        .containsExactlyInAnyOrder("CHANGED", "UNCHANGED");

    // Assert: Final state of lockdown config variable.
    String actualValue = configRepository.findByName(ConfigConst.USER_LOCKDOWN).orElseThrow().getValue();
    assertThat(actualValue).as("Lockdown config variable has wrong value").isEqualTo(expectedConfigValue);

    // Assert: Exactly one LOCKDOWN history event was recorded (duplicates would pollute audit log).
    List<SystemHistory> lockdownEvents = systemHistoryRepository.findAll().stream()
        .filter(e -> EnHistoryWhat.LOCKDOWN.equals(e.getWhat()))
        .toList();
    assertThat(lockdownEvents).as("Exactly one lockdown history event must exist").hasSize(1);
    assertThat(lockdownEvents.getFirst().getParams()).as("Lockdown event has wrong params").isEqualTo(expectedParams);
  }
}
