package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.standard.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.events.UserAccountDeleteRequestEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeRequestEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetRequestEvent;
import org.portfolio.userland.features.user.exceptions.UserTokenAlreadyExistsException;
import org.portfolio.userland.features.user.services.standard.UserDeleteTx;
import org.portfolio.userland.features.user.services.standard.UserEmailTx;
import org.portfolio.userland.features.user.services.standard.UserPasswordTx;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that concurrent creation of same token type for same user is safe - exactly one request must win,
 * all others must fail with {@link UserTokenAlreadyExistsException} without performing their action.
 */
public class UserTokenSendRaceTest extends BaseUserTest {
  /** Services under test. Called directly (not via MockMvc), so each call runs in its own transaction. */
  @Autowired
  private UserPasswordTx userPasswordTx;
  @Autowired
  private UserEmailTx userEmailTx;
  @Autowired
  private UserDeleteTx userDeleteTx;

  /**
   * Verifies that two concurrent requests for password reset link result in exactly one created token and one sent
   * email, while the other request fails as 'token already exists'.
   */
  @Test
  public void onlyOneConcurrentPasswordTokenCreationWins() throws Exception {
    // Arrange: Create active user without any password reset token.
    User user = prepareActiveUser();

    UserPassResetLinkReq req = new UserPassResetLinkReq(user.getEmail(), null);
    List<String> outcomes = race(() -> userPasswordTx.send(req, user));

    assertRaceOutcome(outcomes, user, EnUserTokenType.PASSWORD, UserPasswordResetRequestEvent.class);
  }

  /**
   * Verifies that two concurrent requests for email change link result in exactly one created token and one sent
   * email pair, while the other request fails as 'token already exists'.
   */
  @Test
  public void onlyOneConcurrentEmailTokenCreationWins() throws Exception {
    // Arrange: Create active user without any email change token.
    User user = prepareActiveUser();

    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new@example.com", "Password123!", null);
    List<String> outcomes = race(() -> userEmailTx.send(req, user));

    assertRaceOutcome(outcomes, user, EnUserTokenType.EMAIL, UserEmailChangeRequestEvent.class);
  }

  /**
   * Verifies that two concurrent requests for account deletion link result in exactly one created token and one sent
   * email, while the other request fails as 'token already exists'.
   */
  @Test
  public void onlyOneConcurrentDeleteTokenCreationWins() throws Exception {
    // Arrange: Create active user without any account deletion token.
    User user = prepareActiveUser();

    UserDeleteLinkReq req = new UserDeleteLinkReq("Password123!", null);
    List<String> outcomes = race(() -> userDeleteTx.send(req, user));

    assertRaceOutcome(outcomes, user, EnUserTokenType.DELETE, UserAccountDeleteRequestEvent.class);
  }

  //

  /**
   * Create active user in database. Note such user has no tokens yet.
   * @return Created user.
   */
  private User prepareActiveUser() {
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(user);
    return user;
  }

  /**
   * Run given task in two threads simultaneously and collect their outcomes.
   * @param task Task to run.
   * @return Outcome of each task: "WIN" if it completed normally, "ALREADY_EXISTS" if it failed with
   * {@link UserTokenAlreadyExistsException}, or description of unexpected error.
   */
  private List<String> race(ThrowingRunnable task) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    Callable<String> callable = () -> {
      barrier.await(5, TimeUnit.SECONDS);
      try {
        task.run(); // runs in its own transaction
        return "WIN";
      } catch (UserTokenAlreadyExistsException ex) {
        return "ALREADY_EXISTS";
      }
    };

    ExecutorService pool = Executors.newFixedThreadPool(2);
    List<String> outcomes = new ArrayList<>();
    try {
      List<Future<String>> results = pool.invokeAll(List.of(callable, callable));
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
   * Assert results of a token creation race: exactly one winner, exactly one persisted token and exactly one
   * published request event (thus also only one email will be sent).
   * @param outcomes Outcomes as returned by {@link #race}.
   * @param user User that raced.
   * @param type Type of the token that was raced for.
   * @param expectedEvent Class of the event that is published on successful token creation.
   */
  private <T> void assertRaceOutcome(List<String> outcomes, User user, EnUserTokenType type,
                                 Class<T> expectedEvent) {
    // Assert: Exactly one request won, the other was rejected.
    assertThat(outcomes).as("Exactly one request must win the race").containsExactlyInAnyOrder("WIN", "ALREADY_EXISTS");

    // Assert: Database state - exactly one token of this type exists.
    assertThat(userTokenRepository.findByUserAndType(user.getId(), type))
        .as("Exactly one token of this type should exist")
        .isPresent();

    // Assert: Request event was published exactly once (thus also only one email will be sent). Note the losing
    // transaction rolled back before reaching event publishing, so its event must not be present.
    assertThat(applicationEvents.stream(expectedEvent))
        .as("Request event should be published exactly once")
        .hasSize(1);
  }

  /**
   * Variant of {@link Runnable} that allows checked exceptions, so services can be called without wrapping them
   * in try-catch blocks.
   */
  @FunctionalInterface
  private interface ThrowingRunnable {
    /**
     * Runs the task.
     * @throws Exception If task fails.
     */
    void run() throws Exception;
  }
}
