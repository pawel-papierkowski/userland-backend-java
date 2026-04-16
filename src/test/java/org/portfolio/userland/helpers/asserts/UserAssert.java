package org.portfolio.userland.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.data.UserHistory;
import org.portfolio.userland.features.user.data.UserToken;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assert users and related entities.
 * NOTE: if test do not have @Transactional, you need to use TransactionTemplate:
 * <pre>
 *  private final TransactionTemplate transactionTemplate;
 *  transactionTemplate.execute(_ -> {
 *    User actualUser = ...;
 *    userAssert.assertIt(actualUser, expectedUser);
 *  });
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class UserAssert {
  private static final String[] USER_FIELDS_IGNORE = { "id", "password", "tokens", "history" };
  private static final String[] USER_TOKEN_FIELDS_IGNORE = { "id", "user", "token" };
  private static final String[] USER_HISTORY_FIELDS_IGNORE = { "id", "user", "uuid" };

  /**
   * Assert that two users are same.
   * @param actualUser Actual user.
   * @param expectedUser Expected user.
   */
  public void assertIt(User actualUser, User expectedUser) {
    // Assert standard fields.
    assertThat(actualUser)
        .usingRecursiveComparison()
        .ignoringFields(USER_FIELDS_IGNORE)
        .isEqualTo(expectedUser);

    // Assert fields that need to be asserted separately for various reasons
    assertThat(actualUser.getId()).as("User id is wrong").isGreaterThan(0L);
    assertThat(actualUser.getPassword()).as("User password must be hashed").isNotEqualTo("Password123!");
    assertThat(actualUser.getPassword()).as("User password hash must be BCrypt").startsWith("$2a$"); // Ensure MapStruct + BCrypt hashed the password!

    // Assert collections
    assertTokens(actualUser.getTokens(), expectedUser.getTokens());
    assertHistory(actualUser.getHistory(), expectedUser.getHistory());
  }

  /**
   * Assert all token entries.
   * @param actualTokens Actual token entries.
   * @param expectedTokens Expected token entries.
   */
  private void assertTokens(List<UserToken> actualTokens, List<UserToken> expectedTokens) {
    assertThat(actualTokens.size()).as("User count of tokens is wrong").isEqualTo(expectedTokens.size());
    for (int i=0; i<actualTokens.size(); i++) {
      UserToken actualToken = actualTokens.get(i);
      UserToken expectedToken = expectedTokens.get(i);
      assertTokenEntry(i, actualToken, expectedToken);
    }
  }

  /**
   * Assert one token entry.
   * @param ix Index.
   * @param actualToken Actual token entry.
   * @param expectedToken Expected token entry.
   */
  private void assertTokenEntry(int ix, UserToken actualToken, UserToken expectedToken) {
    assertThat(actualToken)
        .as("Token entry has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_TOKEN_FIELDS_IGNORE)
        .isEqualTo(expectedToken);

    assertThat(actualToken.getId()).as("Token["+ix+"] entry id is wrong").isGreaterThan(0L);
    assertThat(actualToken.getToken()).as("Token["+ix+"] entry string is invalid").matches(ValidConst.REG_EXPR_TOKEN);
  }

  /**
   * Assert all history events.
   * @param actualHistory Actual history.
   * @param expectedHistory Expected history.
   */
  private void assertHistory(List<UserHistory> actualHistory, List<UserHistory> expectedHistory) {
    assertThat(actualHistory.size()).as("User count of history is wrong").isEqualTo(expectedHistory.size());
    for (int i=0; i<actualHistory.size(); i++) {
      UserHistory actualHistoryEvent = actualHistory.get(i);
      UserHistory expectedHistoryEvent = expectedHistory.get(i);
      assertHistoryEvent(i, actualHistoryEvent, expectedHistoryEvent);
    }
  }

  /**
   * Assert one history event.
   * @param ix Index.
   * @param actualHistoryEvent Actual history event.
   * @param expectedHistoryEvent Expected history event.
   */
  private void assertHistoryEvent(int ix, UserHistory actualHistoryEvent, UserHistory expectedHistoryEvent) {
    assertThat(actualHistoryEvent)
        .as("History event has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_HISTORY_FIELDS_IGNORE)
        .isEqualTo(expectedHistoryEvent);

    assertThat(actualHistoryEvent.getId()).as("History["+ix+"] event id is wrong").isGreaterThan(0L);
    assertThat(actualHistoryEvent.getUuid()).as("History["+ix+"] event UUID is invalid").matches(ValidConst.REG_EXPR_UUID);
  }
}
