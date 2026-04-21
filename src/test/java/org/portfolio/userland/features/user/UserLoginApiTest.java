package org.portfolio.userland.features.user;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.services.jwt.JwtService;
import org.portfolio.userland.features.user.dto.login.UserLoginReq;
import org.portfolio.userland.features.user.dto.login.UserLoginResp;
import org.portfolio.userland.features.user.entity.EnHistoryWhat;
import org.portfolio.userland.features.user.entity.Permission;
import org.portfolio.userland.features.user.entity.User;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user login.
 */
public class UserLoginApiTest extends BaseUserTest {
  @Autowired
  private JwtService jwtService;

  @AfterEach
  public void tearDown() {
    cleanDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void loginNoRights() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has no special permissions.
    User expectedUser = userFactory.genUser();
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    UserLoginResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserLoginResp.class);

    // Prepare expected result (user is same, but with new LOGIN history event and JWT entry).
    userHistoryFactory.genHistoryEvent(expectedUser, EnHistoryWhat.LOGIN);
    userJwtFactory.genJwtEntry(expectedUser, actualResp.jwtToken());

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });

    // Assert: Validate it is proper JWT token with correct signature and payload.
    assertThat(jwtService.isTokenValid(actualResp.jwtToken(), expectedUser.getEmail())).as("Token must be valid").isTrue();
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(actualResp.jwtToken());
    Map<String, Object> expectedClaimMap = Maps.newHashMap();
    expectedClaimMap.put("iat", 1775808300L); // issued
    expectedClaimMap.put("exp", 1775894700L); // expires
    expectedClaimMap.put("sub", "test@example.com"); // user account email as subject
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  @Test
  public void loginWithRights() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has special permissions (operator of administration panel).
    Permission permissionRole = permissionRepository.findByName("role").orElseThrow();
    User expectedUser = userFactory.genUser();
    userPermissionFactory.genPermissionEntry(expectedUser, permissionRole, "operator");
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    UserLoginResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserLoginResp.class);

    // Prepare expected result (user is same, but with new LOGIN history event).
    userHistoryFactory.genHistoryEvent(expectedUser, EnHistoryWhat.LOGIN);
    userJwtFactory.genJwtEntry(expectedUser, actualResp.jwtToken());

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });

    // Assert: Validate it is proper JWT token with correct signature and payload.
    assertThat(jwtService.isTokenValid(actualResp.jwtToken(), expectedUser.getEmail())).as("Token must be valid").isTrue();
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(actualResp.jwtToken());
    Map<String, Object> expectedClaimMap = Maps.newHashMap();
    expectedClaimMap.put("iat", 1775808300L); // issued
    expectedClaimMap.put("exp", 1775894700L); // expires
    expectedClaimMap.put("sub", "test@example.com"); // user account email as subject
    expectedClaimMap.put("role", "operator"); // from permission entry
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @Transactional
  public void errWrongPassword() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User expectedUser = userFactory.genUser();
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "wrongPassword");

    // Act: Try to log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password.",
        "Cannot log in as user with email '"+expectedUser.getEmail()+"' due to wrong password.",
        "/api/users/login",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
