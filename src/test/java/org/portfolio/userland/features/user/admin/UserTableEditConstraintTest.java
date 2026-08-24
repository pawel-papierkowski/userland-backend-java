package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataReq;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.repositories.user.UserProfileRepository;
import org.portfolio.userland.features.user.repositories.user.UserRepository;
import org.portfolio.userland.features.user.services.admin.UserTableService;
import org.portfolio.userland.features.user.services.standard.UserHelperService;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests translation of a lost email-uniqueness race in admin user edit into clean domain exception. Uses mocks to
 * simulate the unique constraint violation on <code>users.email</code> at flush time, since triggering it via real
 * concurrency with authenticated operators would be overly complex.
 */
public class UserTableEditConstraintTest {
  private UserRepository userRepository;
  private UserProfileRepository userProfileRepository;
  private UserHelperService userHelperService;
  private ClockService clockService;
  private UserTableService service;

  /**
   * Prepare service under test with mocked collaborators and fake authenticated operator.
   */
  @BeforeEach
  public void setup() {
    userRepository = mock(UserRepository.class);
    userProfileRepository = mock(UserProfileRepository.class);
    userHelperService = mock(UserHelperService.class);
    clockService = mock(ClockService.class);

    // Note: UserTableService inherits collaborator fields from BaseService/BaseUserService, so inject them via reflection.
    service = new UserTableService();
    ReflectionTestUtils.setField(service, "userRepository", userRepository);
    ReflectionTestUtils.setField(service, "userProfileRepository", userProfileRepository);
    ReflectionTestUtils.setField(service, "userHelperService", userHelperService);
    ReflectionTestUtils.setField(service, "clockService", clockService);

    // Fake operator (different account than the edited one).
    CustomUserDetails operator = new CustomUserDetails(999L, true, false, "operator", "operator@test.com", List.of());
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(operator, null, operator.getAuthorities()));
  }

  /**
   * Clear security context after each test.
   */
  @AfterEach
  public void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /**
   * Verifies that violation of the unique email constraint at flush time (lost race against concurrent change) is
   * translated into {@link UserEmailAlreadyExistsException} instead of escaping as raw constraint violation.
   */
  @Test
  public void emailConstraintViolationIsTranslated() {
    long userId = 5L;
    User user = new User();
    user.setId(userId);
    user.setEmail("old@test.com");
    user.setVersion(3L);

    when(userHelperService.resolveUser(userId, false, false)).thenReturn(user);
    when(userRepository.existsByEmail("new@test.com")).thenReturn(false); // fast-path check passes...
    when(userProfileRepository.findById(userId)).thenReturn(Optional.of(new UserProfile()));
    when(clockService.getNowUTC()).thenReturn(LocalDateTime.now());
    // ...but the write loses a race against concurrent change taking this email first.
    doThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"))
        .when(userRepository).flush();

    UserFullDataReq req = UserFullDataReq.builder()
        .id(userId)
        .version(3L)
        .email("new@test.com")
        .build();

    assertThatThrownBy(() -> service.editUserData(req))
        .as("Lost uniqueness race must be translated into clean domain exception")
        .isInstanceOf(UserEmailAlreadyExistsException.class)
        .hasMessageContaining("new@test.com");
  }
}
