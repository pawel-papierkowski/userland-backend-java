package org.portfolio.userland.test.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.entities.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
  private static final String[] USER_FIELDS_IGNORE = { "id", "password", "tokens", "jwts", "history", "permissions" };
  private static final String[] USER_TOKEN_FIELDS_IGNORE = { "id", "user", "token" };
  private static final String[] USER_JWT_FIELDS_IGNORE = { "id", "user" };
  private static final String[] USER_HISTORY_FIELDS_IGNORE = { "id", "user", "uuid" };
  private static final String[] USER_PERMISSION_FIELDS_IGNORE = { "id", "user", "permission", "uuid" };

  private static final Comparator<UserJwt> USER_JWT_COMPARATOR =
      Comparator.comparing(UserJwt::getToken);
  private static final Comparator<UserPermission> USER_PERMISSION_COMPARATOR =
      Comparator.comparing((UserPermission it) -> it.getPermission().getName())
          .thenComparing(UserPermission::getValue);

  /**
   * Assert that two users are same.
   * @param actualUser Actual user.
   * @param expectedUser Expected user.
   */
  public void assertIt(User actualUser, User expectedUser) {
    assertIt("User", actualUser, expectedUser);
  }

  /**
   * Assert that two users are same.
   * @param comment Comment.
   * @param actualUser Actual user.
   * @param expectedUser Expected user.
   */
  public void assertIt(String comment, User actualUser, User expectedUser) {
    if (actualUser == expectedUser) throw new IllegalArgumentException("Actual and expected user must be different instances!");

    // Assert standard fields.
    assertThat(actualUser)
        .as(comment + ": is different")
        .usingRecursiveComparison()
        .ignoringFields(USER_FIELDS_IGNORE)
        .isEqualTo(expectedUser);

    // Assert fields that need to be asserted separately for various reasons
    assertThat(actualUser.getId()).as(comment + ": id is wrong").isGreaterThan(0L);
    assertThat(actualUser.getPassword()).as(comment + ": password must be hashed").isNotEqualTo("Password123!");
    assertThat(actualUser.getPassword()).as(comment + ": password hash must be BCrypt").startsWith("$2a$"); // Ensure MapStruct + BCrypt hashed the password!

    // Assert collections
    assertTokens(comment, actualUser.getTokens(), expectedUser.getTokens());
    assertJwt(comment, actualUser.getJwts(), expectedUser.getJwts());
    assertHistory(comment, actualUser.getHistory(), expectedUser.getHistory());
    assertPermissions(comment, actualUser.getPermissions(), expectedUser.getPermissions());
  }

  //

  /**
   * Assert all token entries.
   * @param actualTokens Actual token entries.
   * @param expectedTokens Expected token entries.
   */
  private void assertTokens(String comment, List<UserToken> actualTokens, List<UserToken> expectedTokens) {
    assertThat(actualTokens.size()).as(comment + ": count of tokens is wrong").isEqualTo(expectedTokens.size());
    for (int i=0; i<actualTokens.size(); i++) {
      UserToken actualToken = actualTokens.get(i);
      UserToken expectedToken = expectedTokens.get(i);
      assertTokenEntry(comment, i, actualToken, expectedToken);
    }
  }

  /**
   * Assert one token entry.
   * @param ix Index.
   * @param actualToken Actual token entry.
   * @param expectedToken Expected token entry.
   */
  private void assertTokenEntry(String comment, int ix, UserToken actualToken, UserToken expectedToken) {
    assertThat(actualToken)
        .as(comment + ": Token entry has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_TOKEN_FIELDS_IGNORE)
        .isEqualTo(expectedToken);

    assertThat(actualToken.getId()).as(comment + ": Token["+ix+"] entry id is wrong").isGreaterThan(0L);
    assertThat(actualToken.getToken()).as(comment + ": Token["+ix+"] entry string is invalid").matches(ValidConst.REG_EXPR_TOKEN);
  }

  //

  /**
   * Assert all JWT entries.
   * @param actualJwts Actual JWT entries.
   * @param expectedJwts Expected JWT entries.
   */
  private void assertJwt(String comment, Set<UserJwt> actualJwts, Set<UserJwt> expectedJwts) {
    assertThat(actualJwts.size()).as(comment + ": count of JWT entries is wrong").isEqualTo(expectedJwts.size());

    List<UserJwt> actualJwtsList = convert(actualJwts, USER_JWT_COMPARATOR);
    List<UserJwt> expectedJwtsList = convert(expectedJwts, USER_JWT_COMPARATOR);
    for (int i=0; i<actualJwtsList.size(); i++) {
      UserJwt actualJwt = actualJwtsList.get(i);
      UserJwt expectedJwt = expectedJwtsList.get(i);
      assertJwtEntry(comment, i, actualJwt, expectedJwt);
    }
  }

  /**
   * Assert one JWT entry.
   * @param ix Index.
   * @param actualJwt Actual JWT entry.
   * @param expectedJwt Expected JWT entry.
   */
  private void assertJwtEntry(String comment, int ix, UserJwt actualJwt, UserJwt expectedJwt) {
    assertThat(actualJwt)
        .as(comment + ": JWT entry has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_JWT_FIELDS_IGNORE)
        .isEqualTo(expectedJwt);

    assertThat(actualJwt.getId()).as(comment + ": Jwt["+ix+"] entry id is wrong").isGreaterThan(0L);
  }

  //

  /**
   * Assert all history events.
   * @param actualHistory Actual history.
   * @param expectedHistory Expected history.
   */
  private void assertHistory(String comment, List<UserHistory> actualHistory, List<UserHistory> expectedHistory) {
    assertThat(actualHistory.size()).as(comment + ": count of history events is wrong").isEqualTo(expectedHistory.size());
    for (int i=0; i<actualHistory.size(); i++) {
      UserHistory actualHistoryEvent = actualHistory.get(i);
      UserHistory expectedHistoryEvent = expectedHistory.get(i);
      assertHistoryEvent(comment, i, actualHistoryEvent, expectedHistoryEvent);
    }
  }

  /**
   * Assert one history event.
   * @param ix Index.
   * @param actualHistoryEvent Actual history event.
   * @param expectedHistoryEvent Expected history event.
   */
  private void assertHistoryEvent(String comment, int ix, UserHistory actualHistoryEvent, UserHistory expectedHistoryEvent) {
    assertThat(actualHistoryEvent)
        .as(comment + ": History event has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_HISTORY_FIELDS_IGNORE)
        .isEqualTo(expectedHistoryEvent);

    assertThat(actualHistoryEvent.getId()).as(comment + ": History["+ix+"] event id is wrong").isGreaterThan(0L);
    assertThat(actualHistoryEvent.getUuid()).as(comment + ": History["+ix+"] event UUID is invalid").matches(ValidConst.REG_EXPR_UUID);
  }

  //

  /**
   * Assert all permission entries.
   * @param actualPermissions Actual permission entries.
   * @param expectedPermissions Expected permission entries.
   */
  private void assertPermissions(String comment, Set<UserPermission> actualPermissions, Set<UserPermission> expectedPermissions) {
    assertThat(actualPermissions.size()).as(comment + ": count of permissions is wrong").isEqualTo(expectedPermissions.size());

    List<UserPermission> actualJwtsList = convert(actualPermissions, USER_PERMISSION_COMPARATOR);
    List<UserPermission> expectedJwtsList = convert(expectedPermissions, USER_PERMISSION_COMPARATOR);
    for (int i=0; i<actualJwtsList.size(); i++) {
      UserPermission actualPermission = actualJwtsList.get(i);
      UserPermission expectedPermission = expectedJwtsList.get(i);
      assertPermissionEntry(comment, i, actualPermission, expectedPermission);
    }
  }

  /**
   * Assert one permission entry.
   * @param ix Index.
   * @param actualPermission Actual permission entry.
   * @param expectedPermission Expected permission entry.
   */
  private void assertPermissionEntry(String comment, int ix, UserPermission actualPermission, UserPermission expectedPermission) {
    assertThat(actualPermission)
        .as(comment + ": Right entry has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_PERMISSION_FIELDS_IGNORE)
        .isEqualTo(expectedPermission);

    assertThat(actualPermission.getId()).as(comment + ": Right["+ix+"] entry id is wrong").isGreaterThan(0L);
    assertThat(actualPermission.getUuid()).as(comment + ": Right["+ix+"] entry UUID is invalid").matches(ValidConst.REG_EXPR_UUID);
  }

  //

  /**
   * Convert set to sorted list, using given comparator for stable sorting.
   * @param set Set.
   * @param comparator For sorting.
   * @return Set as sorted list.
   * @param <T> Type of set and resulting list.
   */
  private <T> List<T> convert(Set<T> set, Comparator<? super T> comparator) {
    return set.stream().sorted(comparator).toList();
  }
}
