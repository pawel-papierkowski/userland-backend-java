package org.portfolio.userland.system.lockdown;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.BaseSystemTest;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.history.entity.EnHistoryWhat;
import org.portfolio.userland.system.history.entity.EnHistoryWho;
import org.portfolio.userland.system.history.entity.SystemHistory;
import org.portfolio.userland.system.lockdown.dto.EnSystemLockdownState;
import org.portfolio.userland.system.lockdown.dto.SystemLockdownReq;
import org.portfolio.userland.system.lockdown.dto.SystemLockdownResp;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for lockdown endpoints.
 */
public class SystemLockdownApiTest extends BaseSystemTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  @Transactional
  public void lockdownOff() throws Exception {
    // Arrange: none needed.

    // Act: Get system lockdown data.
    MvcResult mvcResult = mockMvc.perform(get("/api/system/lockdown"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    SystemLockdownResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SystemLockdownResp.class);
    SystemLockdownResp expectedResp = new SystemLockdownResp(EnSystemLockdownState.OFF);
    assertThat(actualResp).as("Response is invalid").isEqualTo(expectedResp);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  @Transactional
  public void lockdownOn() throws Exception {
    // Arrange: Activate lockdown.
    configRepository.updateValueByName(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    // Act: Get system lockdown data.
    MvcResult mvcResult = mockMvc.perform(get("/api/system/lockdown"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    SystemLockdownResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), SystemLockdownResp.class);
    SystemLockdownResp expectedResp = new SystemLockdownResp(EnSystemLockdownState.ON);
    assertThat(actualResp).as("Response is invalid").isEqualTo(expectedResp);
  }

  @Test
  @WithMockCustomUser
  @Transactional
  public void errLockdownOff() throws Exception {
    // Arrange: none needed.

    // Act: Get system lockdown data. User has no admin rights.
    MvcResult mvcResult = mockMvc.perform(get("/api/system/lockdown"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.FORBIDDEN.value());
    // Assert: correct problem detail is present.
    problemDetailService.assertPdForbidden(mvcResult, "/api/system/lockdown");
  }

  @Test
  @WithMockCustomUser
  @Transactional
  public void errLockdownOn() throws Exception {
    // Arrange: Activate lockdown.
    configRepository.updateValueByName(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    // Act: Get system lockdown data. User has no admin rights.
    MvcResult mvcResult = mockMvc.perform(get("/api/system/lockdown"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "System lockdown is in effect.",
        "User with email 'test@example.com' cannot access endpoint due to lockdown.",
        "/api/system/lockdown",
        "https://api.userland.org/errors/user/lockdown",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  @WithMockCustomUser(email = "admin@test.com", authorities = { "ROLE_ADMIN" })
  public void activateLockdown() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Add two users, one standard and other admin. Both are logged in.
    User expectedUserStandard = userFactory.genRandUserLogged();
    userRepository.save(expectedUserStandard);
    User expectedUserAdmin = userFactory.genRandUserLogged(Map.of("role", "admin"));
    expectedUserAdmin.setEmail("admin@test.com");
    User userAdmin = userRepository.save(expectedUserAdmin);

    // Arrange: Request.
    SystemLockdownReq req = new SystemLockdownReq(EnSystemLockdownState.ON);

    // Act: Activate system lockdown.
    MvcResult mvcResult = mockMvc.perform(post("/api/system/lockdown")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Assert: State of lockdown.
    Config config = configRepository.findByName(ConfigConst.USER_LOCKDOWN).orElseThrow();
    assertThat(config.getValue()).as("User lockdown config variable has wrong value").isEqualTo("1");

    // Prepare expected result.
    expectedUserStandard.getJwts().clear();

    // Assert: State of users. Standard user should have their jwt data removed.
    transactionTemplate.execute(_ -> {
      User actualUserAdmin = userRepository.findByEmail(expectedUserAdmin.getEmail()).orElseThrow();
      User actualUserStandard = userRepository.findByEmail(expectedUserStandard.getEmail()).orElseThrow();

      userAssert.assertIt("Admin User", actualUserAdmin, expectedUserAdmin);
      userAssert.assertIt("Standard User", actualUserStandard, expectedUserStandard);
      return null;
    });

    // Assert: Lockdown event present in system history and assigned to correct user.
    SystemHistory expectedHistoryEvent = systemHistoryFactory.genHistoryEvent(userAdmin, EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");
    systemHistoryAssert.assertAll(List.of(expectedHistoryEvent));
  }

  @Test
  @WithMockCustomUser(email = "admin@test.com", authorities = { "ROLE_ADMIN" })
  @Transactional
  public void deactivateLockdown() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Add admin user.
    User expectedUserAdmin = userFactory.genRandUserLogged(Map.of("role", "admin"));
    expectedUserAdmin.setEmail("admin@test.com");
    User userAdmin = userRepository.save(expectedUserAdmin);
    // Arrange: Activate lockdown.
    configRepository.updateValueByName(ConfigConst.USER_LOCKDOWN, ConfigConst.TRUE);

    // Arrange: Request.
    SystemLockdownReq req = new SystemLockdownReq(EnSystemLockdownState.OFF);

    // Act: Activate system lockdown.
    MvcResult mvcResult = mockMvc.perform(post("/api/system/lockdown")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Assert: State of lockdown.
    Config config = configRepository.findByName(ConfigConst.USER_LOCKDOWN).orElseThrow();
    assertThat(config.getValue()).as("User lockdown config variable has wrong value").isEqualTo("0");

    // Assert: Lockdown event present in system history and assigned to correct user.
    SystemHistory expectedHistoryEvent = systemHistoryFactory.genHistoryEvent(userAdmin, EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "OFF");
    systemHistoryAssert.assertAll(List.of(expectedHistoryEvent));
  }
}
