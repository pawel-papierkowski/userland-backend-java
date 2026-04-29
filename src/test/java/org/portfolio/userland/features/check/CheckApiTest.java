package org.portfolio.userland.features.check;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.constants.EnAppProfile;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.check.data.CheckInfoResp;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Test check endpoints.
 * Note endpoints that require login/permissions have manually arranged user instead of <code>@WithMockCustomUser.</code>
 * This is deliberate - we are testing full flow here without mocking Spring's user detail.
 */
public class CheckApiTest extends BaseCheckTest {
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
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
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
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
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
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
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
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
  }

  //

  @Test
  void requestExceptionEndpoint() throws Exception {
    // No arrange here.

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/exception"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    // Assert: correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        "An unexpected error occurred while processing your request.",
        "/api/checks/exception",
        "https://api.general.org/errors/internal",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
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
  void errMalformedToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // No arrange here.

    // Act: This endpoint expects token, but there is no token. So Spring rejects it (throwing 401 Unauthorized).
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/must-be-logged")
            .header("Authorization", "Bearer MALFORMED_TOKEN"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    // Assert: Check the header required by RFC 6750.
    String authHeader = mvcResult.getResponse().getHeader("WWW-Authenticate");
    assertThat(authHeader)
        .as("WWW-Authenticate header is missing or incorrect")
        .isEqualTo("Bearer error=\"invalid_token\"");
    // Assert: correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        "Bearer token is invalid or malformed and cannot be used.",
        "/api/checks/must-be-logged",
        "https://api.userland.org/errors/user/malformedToken",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  void errNoJwtInDatabase() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user and token. Note UserJwt entry is not added. This will emulate state when we logged in
    // and then logged out. Token is still valid, but we know it is revoked because it is missing from UserJwt table.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userHistoryFactory.genHistoryEvent(user, EnUserHistoryWhat.LOGIN, "");
    String token = jwtService.generateToken(user);
    userHistoryFactory.genHistoryEvent(user, EnUserHistoryWhat.LOGOUT, "");
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
        "User with email '"+user.getEmail()+"' cannot access endpoint due to lockdown.",
        "/api/checks/must-be-logged",
        "https://api.userland.org/errors/user/lockdown",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  void requestInfo() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user, token and JWT entry in database, emulating user login.
    User user = userFactory.genRandUserLogged(Map.of("role", "admin"));
    userRepository.save(user);
    String token = userJwtRepository.findAll().getFirst().getToken();

    // Act: Perform the request using MockMvc.
    MvcResult mvcResult = mockMvc.perform(get("/api/checks/info")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
    // Assert: Endpoint response. Note we do not check bootAt and version exactly (as they change).
    CheckInfoResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CheckInfoResp.class);
    CheckInfoResp expectedResp = new CheckInfoResp("UserLand", clockService.getNowUTC(), actualResp.bootAt(), actualResp.version(), EnAppProfile.TEST);
    assertThat(actualResp).as("System info is invalid").isEqualTo(expectedResp);
    assertThat(actualResp.version()).as("Version is invalid").matches(ValidConst.REG_EXPR_VERSION); // ensure @project.version@ is correctly resolved
  }
}
