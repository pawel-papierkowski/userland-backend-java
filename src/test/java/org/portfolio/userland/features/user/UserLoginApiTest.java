package org.portfolio.userland.features.user;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.constants.UserConfigConst;
import org.portfolio.userland.features.user.dto.login.UserLoginReq;
import org.portfolio.userland.features.user.dto.login.UserLoginResp;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
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
  @Autowired
  private ConfigService configService;

  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void loginNoRights() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has no special permissions.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .header("X-Forwarded-For", "192.168.1.50") // Simulate proxy IP
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) // Simulate Browser
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    UserLoginResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserLoginResp.class);

    // Prepare expected result (user is same, but with new LOGIN history event and JWT entry).
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.LOGIN, "IP: '192.168.1.50', User-Agent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'");
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
    expectedClaimMap.put("iat", 1775815500L); // issued
    expectedClaimMap.put("exp", 1775837100L); // expires
    expectedClaimMap.put("sub", "test@example.com"); // user account email as subject
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  @Test
  public void loginWithRights() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has special permissions (operator of administration panel).
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE, Map.of("role", "operator"));
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .header("X-Forwarded-For", "192.168.1.50") // Simulate proxy IP
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) // Simulate Browser
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    UserLoginResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserLoginResp.class);

    // Prepare expected result (user is same, but with new LOGIN history event).
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.LOGIN, "IP: '192.168.1.50', User-Agent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'");
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
    expectedClaimMap.put("iat", 1775815500L); // issued
    expectedClaimMap.put("exp", 1775837100L); // expires
    expectedClaimMap.put("sub", "test@example.com"); // user account email as subject
    expectedClaimMap.put("role", "operator"); // from permission entry
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  @Test
  public void loginWithRightsWhenLockdown() throws Exception {
    // Login endpoint has special handling of lockdown, so we test it separately.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has special permissions (operator of administration panel).
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE, Map.of("role", "operator"));
    userRepository.save(expectedUser);

    // Arrange: lock down system. Anyone can still access login endpoint and users with correct permissions should be
    // still able to log in.
    configService.set(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .header("X-Forwarded-For", "192.168.1.50") // Simulate proxy IP
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) // Simulate Browser
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    UserLoginResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserLoginResp.class);

    // Prepare expected result (user is same, but with new LOGIN history event).
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.LOGIN, "IP: '192.168.1.50', User-Agent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'");
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
    expectedClaimMap.put("iat", 1775815500L); // issued
    expectedClaimMap.put("exp", 1775837100L); // expires
    expectedClaimMap.put("sub", "test@example.com"); // user account email as subject
    expectedClaimMap.put("role", "operator"); // from permission entry
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  @Test
  public void loginCustomExpiration() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has custom expiration in user config.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userConfigFactory.genConfig(expectedUser, UserConfigConst.JWT_EXPIRE, "60"); // 1 hour
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req))
            .header("X-Forwarded-For", "192.168.1.50") // Simulate proxy IP
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) // Simulate Browser
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    UserLoginResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserLoginResp.class);

    // Prepare expected result (user is same, but with new LOGIN history event and JWT entry).
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.LOGIN, "IP: '192.168.1.50', User-Agent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'");
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
    expectedClaimMap.put("iat", 1775815500L); // issued
    expectedClaimMap.put("exp", 1775819100L); // expires in 1 hour
    expectedClaimMap.put("sub", "test@example.com"); // user account email as subject
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @Transactional
  public void errMissingAccount() throws Exception {
    // Refuse if user do not exist.
    clock.setFixedTime("2026-04-08T10:00:00Z");

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "5oMeP@ssw0rd");

    // Act: Try to log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: proper problem detail is present. Yes, "Wrong password." error is correct.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password.",
        "Cannot login due to wrong password.",
        "/api/users/login",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errWrongPassword() throws Exception {
    // Refuse if user provided wrong password.
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
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
    // Assert: proper problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password.",
        "Cannot login due to wrong password.",
        "/api/users/login",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errUserLocked() throws Exception {
    // Refuse if user is locked.
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    expectedUser.setLocked(true);
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Try to log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: proper problem detail is present. Yes, "Wrong password." error is correct.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password.",
        "Cannot login due to wrong password.",
        "/api/users/login",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errUserInvalidStatus() throws Exception {
    // Refuse if user has invalid status (cannot log in when user is PENDING).
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Try to log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: proper problem detail is present. Yes, "Wrong password." error is correct.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password.",
        "Cannot login due to wrong password.",
        "/api/users/login",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errLockdown() throws Exception {
    // Login endpoint has special handling of lockdown, so we test it separately.
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(user);

    // Arrange: lock down system. Anyone can still access login endpoint, but if user has no appropriate permissions,
    // login attempt will be rejected.
    configService.set(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    clock.setFixedTime("2026-04-10T10:05:00Z");
    // Arrange: Create login request.
    UserLoginReq req = new UserLoginReq("test@example.com", "Password123!");

    // Act: Try to log in user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: proper problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "System lockdown is in effect.",
        "User with email '"+user.getEmail()+"' cannot access endpoint due to lockdown.",
        "/api/users/login",
        "https://api.userland.org/errors/user/lockdown",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
