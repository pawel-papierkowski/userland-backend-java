package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeConfirmReq;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.events.UserAlreadyRegisteredEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeConfirmEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.repositories.token.UserTokenRepository;
import org.portfolio.userland.features.user.services.standard.UserEmailService;
import org.portfolio.userland.features.user.services.standard.UserRegisterService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that concurrent usage of same email address is safe - exactly one write must win, all others must be
 * handled gracefully (via 'already registered' flow or clean {@link UserEmailAlreadyExistsException}), never
 * surfacing a raw constraint violation.
 */
public class UserEmailRaceTest extends BaseUserTest {
  /** Services under test. Called directly (not via MockMvc), so each call runs in its own transaction. */
  @Autowired
  private UserRegisterService userRegisterService;
  @Autowired
  private UserEmailService userEmailService;

  /** Email used by both races. */
  private static final String RACED_EMAIL = "raced@example.com";

  /**
   * Reset state of database so test does not interfere with other tests (or its own previous runs).
   */
  @BeforeEach
  protected void resetDb() {
    resetDatabase();
  }

  /**
   * Verifies that two concurrent registrations with same email result in exactly one created account, while the
   * other request is gracefully routed to the 'already registered' flow - no raw constraint violation may escape.
   */
  @Test
  public void onlyOneConcurrentRegistrationWins() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Prepare identical registration requests.
    UserRegisterReq req = UserRegisterReq.builder()
        .username("Jane")
        .email(RACED_EMAIL)
        .password("Password123!")
        .lang("en")
        .name("Jane")
        .surname("Doe")
        .build();

    // Act: Run both registrations concurrently.
    List<String> outcomes = runConcurrently(() -> {
      userRegisterService.register(req);
      return "WIN_A";
    }, () -> {
      userRegisterService.register(req);
      return "WIN_B";
    });

    // Assert: Both requests returned normally - no raw constraint violation may escape. The loser was routed to
    // the graceful 'already registered' flow, which also returns normally.
    assertThat(outcomes).as("No registration attempt may end with unexpected error")
        .containsExactlyInAnyOrder("WIN_A", "WIN_B");

    // Assert: Database state - exactly one account was created.
    List<User> users = userRepository.findAll().stream().filter(u -> RACED_EMAIL.equals(u.getEmail())).toList();
    assertThat(users).as("Exactly one account with raced email must exist").hasSize(1);

    // Assert: Exactly one registration happened (thus also only one activation link email will be sent).
    assertThat(applicationEvents.stream(UserRegisteredEvent.class))
        .as("Registration event should be published exactly once")
        .hasSize(1);

    // Assert: The loser was routed to 'already registered' flow (either via fast-path pre-check or via unique
    // constraint violation), so informational email was triggered at most once.
    assertThat(applicationEvents.stream(UserAlreadyRegisteredEvent.class))
        .as("Already registered event should be published at most once")
        .hasSizeLessThanOrEqualTo(1);
  }

  /**
   * Verifies that two users concurrently confirming email change to the SAME new address results in exactly one
   * performed change, while the other request fails cleanly with {@link UserEmailAlreadyExistsException}.
   */
  @Test
  public void onlyOneConcurrentEmailChangeConfirmWins() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create two active users, each holding an email change token for the same new address.
    User userA = prepareUserWithEmailChangeToken();
    User userB = prepareUserWithEmailChangeToken();
    String tokenA = getTokenString(userA);
    String tokenB = getTokenString(userB);

    // Act: Run both confirmations concurrently. Note each thread uses its own token, but both target same address.
    List<String> outcomes = runConcurrently(() -> {
      userEmailService.confirm(new UserEmailChangeConfirmReq(tokenA));
      return "WIN";
    }, () -> {
      userEmailService.confirm(new UserEmailChangeConfirmReq(tokenB));
      return "WIN";
    });

    // Assert: Exactly one confirmation succeeded, the other failed cleanly as 'email already exists'.
    assertThat(outcomes).as("Exactly one confirmation must win the race")
        .containsExactlyInAnyOrder("WIN", "EMAIL_EXISTS");

    // Assert: Database state - exactly one user owns the raced address now, and only winner's token is consumed
    // (the losing transaction rolled back, restoring its own token consumption).
    User actualUserA = userRepository.findById(userA.getId()).orElseThrow();
    User actualUserB = userRepository.findById(userB.getId()).orElseThrow();
    boolean aWon = RACED_EMAIL.equals(actualUserA.getEmail());
    assertThat(aWon || RACED_EMAIL.equals(actualUserB.getEmail()))
        .as("Exactly one user must own the raced email now").isTrue();
    assertThat(RACED_EMAIL.equals(actualUserA.getEmail()) && RACED_EMAIL.equals(actualUserB.getEmail()))
        .as("Both users cannot own the raced email").isFalse();

    UserTokenRepository tokenRepo = userTokenRepository;
    assertThat(tokenRepo.findByTypeAndToken(EnUserTokenType.EMAIL, aWon ? tokenB : tokenA))
        .as("Loser's token must be restored by rollback").isPresent();
    assertThat(tokenRepo.findByTypeAndToken(EnUserTokenType.EMAIL, aWon ? tokenA : tokenB))
        .as("Winner's token must be consumed").isEmpty();

    // Assert: Confirmation event was published exactly once (thus also only one confirmation email will be sent).
    assertThat(applicationEvents.stream(UserEmailChangeConfirmEvent.class))
        .as("Confirmation event should be published exactly once")
        .hasSize(1);
  }

  //

  /**
   * Create active user with valid email change token targeting the raced address.
   * @return Created (persisted) user.
   */
  private User prepareUserWithEmailChangeToken() {
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userTokenFactory.genTokenEntry(user, EnUserTokenType.EMAIL, null, RACED_EMAIL);
    return userRepository.save(user);
  }

  /**
   * Get token string of the single email change token of given user.
   * @param user User.
   * @return Token string.
   */
  private String getTokenString(User user) {
    return user.getTokens().getFirst().getToken();
  }

  /**
   * Run two tasks in separate threads simultaneously and collect their outcomes.
   * @param taskA First task.
   * @param taskB Second task.
   * @return Outcome of each task: returned value if it completed normally, "EMAIL_EXISTS" if it failed with
   * {@link UserEmailAlreadyExistsException}, or "UNEXPECTED: ..." for any other error.
   */
  private List<String> runConcurrently(Callable<String> taskA, Callable<String> taskB) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    List<Callable<String>> tasks = List.of(
        guard(barrier, taskA),
        guard(barrier, taskB));

    ExecutorService pool = Executors.newFixedThreadPool(2);
    List<String> outcomes = new ArrayList<>();
    try {
      List<Future<String>> results = pool.invokeAll(tasks);
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
   * Wrap task so it waits on barrier first and converts expected race-loss exception into outcome string.
   * @param barrier Barrier to wait on.
   * @param task Task to wrap.
   * @return Guarded task.
   */
  private Callable<String> guard(CyclicBarrier barrier, Callable<String> task) {
    return () -> {
      barrier.await(5, TimeUnit.SECONDS);
      try {
        return task.call(); // runs in its own transaction
      } catch (UserEmailAlreadyExistsException ex) { // expected outcome of losing the race
        return "EMAIL_EXISTS";
      }
    };
  }
}
