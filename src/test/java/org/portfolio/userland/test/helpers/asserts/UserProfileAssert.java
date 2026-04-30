package org.portfolio.userland.test.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assert user profile.
 */
@Service
@RequiredArgsConstructor
public class UserProfileAssert {
  private static final String[] USERPROFILE_FIELDS_IGNORE = { "id", "user" };

  /**
   * Assert that two user profiles are same.
   * @param actualUserProfile Actual user profile.
   * @param expectedUserProfile Expected user profile.
   */
  public void assertIt(UserProfile actualUserProfile, UserProfile expectedUserProfile) {
    assertIt("User", actualUserProfile, expectedUserProfile);
  }

  /**
   * Assert that two user profiles are same.
   * @param comment Comment.
   * @param actualUserProfile Actual user.
   * @param expectedUserProfile Expected user.
   */
  public void assertIt(String comment, UserProfile actualUserProfile, UserProfile expectedUserProfile) {
    if (actualUserProfile == expectedUserProfile) throw new IllegalArgumentException("Actual and expected user profile must be different instances!");

    // Assert standard fields.
    assertThat(actualUserProfile)
        .as(comment + ": is different")
        .usingRecursiveComparison()
        .ignoringFields(USERPROFILE_FIELDS_IGNORE)
        .isEqualTo(expectedUserProfile);

    // Assert certain fields manually.
    assertThat(actualUserProfile.getId()).as(comment + ": id is wrong").isGreaterThan(0L);
    assertThat(actualUserProfile.getId()).as(comment + ": mismatch of id").isEqualTo(actualUserProfile.getUser().getId());
  }
}
