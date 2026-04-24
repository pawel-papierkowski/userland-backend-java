package org.portfolio.userland.features.check;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.entities.EnHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.jwt.JwtService;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Test endpoints with JWT token.
 */
public class CheckWithJwtTest extends BaseUserTest {
  /** Real service to generate a valid token. */
  @Autowired
  private JwtService jwtService;

  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  void requestUnsecuredEndpoint() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // No arrange here.

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/alive"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void requestSecuredEndpointWithValidToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user, token and JWT entry in database, emulating user login.
    User user = userFactory.genRandUserLogged();
    userRepository.save(user);
    String token = userJwtRepository.findAll().getFirst().getToken();

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void requestAdminEndpointWithValidToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user, token and JWT entry in database, emulating user login.
    User user = userFactory.genRandUserLogged(Map.of("role", "admin"));
    userRepository.save(user);
    String token = userJwtRepository.findAll().getFirst().getToken();

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-admin")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
  }

  //

  @Test
  @Transactional
  void lockdownSecuredEndpointAsAdmin() throws Exception { // TODO
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create an admin user, token and JWT entry in database, emulating user login.
    User user = userFactory.genRandUserLogged(Map.of("role", "admin"));
    userRepository.save(user);
    String token = userJwtRepository.findAll().getFirst().getToken();

    // Arrange: Lock down system.
    configRepository.updateValueByName(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  void errWithoutToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // No arrange here.

    // Act: This endpoint expects token, but there is no token. So Spring rejects it (throwing 401 Unauthorized).
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    // Assert: correct problem detail is present.
    problemDetailService.assertPdUnauthorized(mvcResult, "/api/checks/must-be-logged");
  }

  @Test
  void errNoJwtInDatabase() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user and token. Note UserJwt entry is not added. This will emulate state when we logged in
    // and then logged out. Token is still valid, but we know it is revoked because it is missing from UserJwt table.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.LOGIN);
    String token = jwtService.generateToken(user);
    userHistoryFactory.genHistoryEvent(user, EnHistoryWhat.LOGOUT);
    userRepository.save(user);

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    // Assert: correct problem detail is present.
    problemDetailService.assertPdUnauthorized(mvcResult, "/api/checks/must-be-logged");
  }

  @Test
  void errPendingUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a real user.
    User user = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE));
    // Arrange: Create a real token.
    String token = jwtService.generateToken(user);

    // Arrange: Now set user back to PENDING.
    clock.setFixedTime("2026-04-10T11:00:00Z");
    user.setStatus(EnUserStatus.PENDING);
    userRepository.save(user);

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    // Assert: correct problem detail is present.
    problemDetailService.assertPdUnauthorized(mvcResult, "/api/checks/must-be-logged");
  }

  @Test
  void errLockedUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a real user.
    User user = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE));
    // Arrange: Create a real token.
    String token = jwtService.generateToken(user);

    // Arrange: Now lock user.
    clock.setFixedTime("2026-04-10T11:00:00Z");
    user.setLocked(true);
    userRepository.save(user);

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    // Assert: correct problem detail is present.
    problemDetailService.assertPdUnauthorized(mvcResult, "/api/checks/must-be-logged");
  }

  @Test
  void errNoAdmin() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user, token and JWT entry in database, emulating user login. No admin rights.
    User user = userFactory.genRandUserLogged();
    userRepository.save(user);
    String token = userJwtRepository.findAll().getFirst().getToken();

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-admin")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.FORBIDDEN.value());
    // Assert: correct problem detail is present.
    problemDetailService.assertPdForbidden(mvcResult, "/api/checks/must-be-admin");
  }

  //

  @Test
  @Transactional
  void errLockdownUnsecuredEndpoint() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Lock down system.
    configRepository.updateValueByName(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/alive"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "System lockdown is in effect.",
        "System lockdown is in effect.",
        "/api/checks/alive",
        "https://api.userland.org/errors/system/lockdown",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  void errLockdownSecuredEndpoint() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user, token and JWT entry in database, emulating user login.
    User user = userFactory.genRandUserLogged();
    userRepository.save(user);
    String token = userJwtRepository.findAll().getFirst().getToken();

    // Arrange: Lock down system.
    configRepository.updateValueByName(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "System lockdown is in effect.",
        "User with email '"+user.getEmail()+"' cannot access endpoint: lockdown.",
        "/api/checks/must-be-logged",
        "https://api.userland.org/errors/user/lockdown",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
