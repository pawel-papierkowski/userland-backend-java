package org.portfolio.userland.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.dto.lockdown.EnSystemLockdownState;
import org.portfolio.userland.system.dto.lockdown.SystemLockdownReq;
import org.portfolio.userland.system.dto.lockdown.SystemLockdownResp;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
        "User with email 'test@example.com' cannot access endpoint: lockdown.",
        "/api/system/lockdown",
        "https://api.userland.org/errors/user/lockdown",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  @Transactional
  public void activateLockdown() throws Exception {
    // Arrange: Request.
    SystemLockdownReq req = new SystemLockdownReq(EnSystemLockdownState.ON);

    // Act: Activate system lockdown.
    MvcResult mvcResult = mockMvc.perform(post("/api/system/lockdown")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: State of lockdown.
    Config config = configRepository.findByName(ConfigConst.USER_LOCKDOWN).orElseThrow();
    assertThat(config.getValue()).as("User lockdown config variable has wrong value").isEqualTo("1");
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  @Transactional
  public void deactivateLockdown() throws Exception {
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
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: State of lockdown.
    Config config = configRepository.findByName(ConfigConst.USER_LOCKDOWN).orElseThrow();
    assertThat(config.getValue()).as("User lockdown config variable has wrong value").isEqualTo("0");
  }
}
