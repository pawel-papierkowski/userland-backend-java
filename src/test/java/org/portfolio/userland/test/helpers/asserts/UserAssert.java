package org.portfolio.userland.test.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.entities.*;
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
  private static final String[] USER_FIELDS_IGNORE = { "id", "password", "tokens", "jwt", "history", "permissions" };
  private static final String[] USER_TOKEN_FIELDS_IGNORE = { "id", "user", "token" };
  private static final String[] USER_JWT_FIELDS_IGNORE = { "id", "user" };
  private static final String[] USER_HISTORY_FIELDS_IGNORE = { "id", "user", "uuid" };
  private static final String[] USER_PERMISSION_FIELDS_IGNORE = { "id", "user", "permission", "uuid" };

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
    assertJwt(actualUser.getJwt(), expectedUser.getJwt());
    assertHistory(actualUser.getHistory(), expectedUser.getHistory());
    assertPermissions(actualUser.getPermissions(), expectedUser.getPermissions());
  }

  //

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

  //

  /**
   * Assert all JWT entries.
   * @param actualJwts Actual JWT entries.
   * @param expectedJwts Expected JWT entries.
   */
  private void assertJwt(List<UserJwt> actualJwts, List<UserJwt> expectedJwts) {
    assertThat(actualJwts.size()).as("User count of JWT entries is wrong").isEqualTo(expectedJwts.size());
    for (int i=0; i<actualJwts.size(); i++) {
      UserJwt actualJwt = actualJwts.get(i);
      UserJwt expectedJwt = expectedJwts.get(i);
      assertJwtEntry(i, actualJwt, expectedJwt);
    }
  }

  /**
   * Assert one JWT entry.
   * @param ix Index.
   * @param actualJwt Actual JWT entry.
   * @param expectedJwt Expected JWT entry.
   */
  private void assertJwtEntry(int ix, UserJwt actualJwt, UserJwt expectedJwt) {
    assertThat(actualJwt)
        .as("JWT entry has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_JWT_FIELDS_IGNORE)
        .isEqualTo(expectedJwt);

    assertThat(actualJwt.getId()).as("Jwt["+ix+"] entry id is wrong").isGreaterThan(0L);
  }

  //

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

  //

  /**
   * Assert all permission entries.
   * @param actualRights Actual permission entries.
   * @param expectedRights Expected permission entries.
   */
  private void assertPermissions(List<UserPermission> actualRights, List<UserPermission> expectedRights) {
    assertThat(actualRights.size()).as("User count of permissions is wrong").isEqualTo(expectedRights.size());
    for (int i=0; i<actualRights.size(); i++) {
      UserPermission actualRight = actualRights.get(i);
      UserPermission expectedRight = expectedRights.get(i);
      assertPermissionEntry(i, actualRight, expectedRight);
    }
  }

  /**
   * Assert one permission entry.
   * @param ix Index.
   * @param actualRight Actual permission entry.
   * @param expectedRight Expected permission entry.
   */
  private void assertPermissionEntry(int ix, UserPermission actualRight, UserPermission expectedRight) {
    assertThat(actualRight)
        .as("Right entry has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_PERMISSION_FIELDS_IGNORE)
        .isEqualTo(expectedRight);

    assertThat(actualRight.getId()).as("Right["+ix+"] entry id is wrong").isGreaterThan(0L);
    assertThat(actualRight.getUuid()).as("Right["+ix+"] entry UUID is invalid").matches(ValidConst.REG_EXPR_UUID);
  }
}
