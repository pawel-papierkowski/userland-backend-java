package org.portfolio.userland.common.services.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.entities.EnHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Test endpoint with JWT token.
 */
public class JwtControllerTest extends BaseUserTest {
  /** Real service to generate a valid token. */
  @Autowired
  private JwtService jwtService;

  @AfterEach
  public void tearDown() {
    cleanDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  void requestSecuredEndpointWithValidToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a user, token and JWT entry in database, emulating user login.
    User user = userFactory.genUserLogged();
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
    Permission permissionRole = permissionRepository.findByName("role").orElseThrow();
    User user = userFactory.genUserLogged();
    userPermissionFactory.genPermissionEntry(user, permissionRole, "admin");
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
  void errNoToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

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
    User user = userFactory.genUser();
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
    User user = userRepository.save(userFactory.genUser());
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
    User user = userRepository.save(userFactory.genUser());
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
    User user = userFactory.genUserLogged();
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
}
