package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.standard.register.UserActivateReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.exceptions.UserTokenAlreadyUsedException;
import org.portfolio.userland.features.user.services.standard.UserRegisterTx;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that concurrent usage of the same token is safe - exactly one request must win,
 * all others must fail with {@link UserTokenAlreadyUsedException} without performing their action.
 */
public class UserTokenRaceTest extends BaseUserTest {
  /** Service under test. Called directly (not via MockMvc), so each call runs in its own transaction. */
  @Autowired
  private UserRegisterTx userRegisterTx;

  /**
   * Verifies that when two threads race to activate a user with the same token,
   * exactly one activation happens and the second attempt fails as 'already used'.
   */
  @Test
  public void onlyOneConcurrentTokenUsageWins() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create pending user, profile and activation token.
    User user = userFactory.genUser(EnUserStatus.PENDING);
    userProfileRepository.save(userProfileFactory.genRandProfile(user));
    String tokenStr = user.getTokens().getFirst().getToken();

    clock.setFixedTime("2026-04-10T10:05:00Z");
    UserActivateReq req = new UserActivateReq(tokenStr, null);

    // Arrange: Prepare two threads using the same token. Barrier ensures they start as simultaneously as possible.
    CyclicBarrier barrier = new CyclicBarrier(2);
    var task = (Callable<String>) () -> {
      barrier.await(5, TimeUnit.SECONDS);
      try {
        userRegisterTx.activate(req); // runs in its own transaction
        return "WIN";
      } catch (UserTokenAlreadyUsedException ex) {
        return "ALREADY_USED";
      }
    };

    // Act: Run both threads concurrently.
    ExecutorService pool = Executors.newFixedThreadPool(2);
    List<String> outcomes = new ArrayList<>();
    try {
      List<Future<String>> results = pool.invokeAll(List.of(task, task));
      for (Future<String> f : results) {
        try {
          outcomes.add(f.get(10, TimeUnit.SECONDS));
        } catch (ExecutionException ex) {
          outcomes.add("UNEXPECTED: "+ex.getCause());
        }
      }

      // Assert: Exactly one request won, the other was rejected.
      assertThat(outcomes).as("Exactly one request must win the race").containsExactlyInAnyOrder("WIN", "ALREADY_USED");
    } finally {
      pool.shutdownNow();
    }

    // Assert: Database state - user is activated exactly once and token is gone.
    User actualUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(actualUser.getStatus())
        .as("User should be activated")
        .isEqualTo(EnUserStatus.ACTIVE);
    assertThat(userTokenRepository.findByUserAndType(user.getId(), EnUserTokenType.ACTIVATE))
        .as("Activation token should be gone")
        .isEmpty();

    // Assert: Activation event was published exactly once (thus also only one confirmation email will be sent).
    assertThat(applicationEvents.stream(UserActivatedEvent.class))
        .as("Activation event should be published exactly once")
        .hasSize(1);
  }
}
