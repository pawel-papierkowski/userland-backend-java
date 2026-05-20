package org.portfolio.userland.features.user;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.login.UserProlongResp;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user prolong.
 */
public class UserProlongApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void prolongUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that is already logged in.
    User expectedUser = userFactory.genUserLogged();
    String token = expectedUser.getJwts().stream().toList().getFirst().getToken();
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T12:00:00Z");
    // Act: prolong user session.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/prolong")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
    UserProlongResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserProlongResp.class);

    // Prepare expected result (user is same, but with new PROLONG history event and new JWT entry).
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.PROLONG, "");
    expectedUser.getJwts().clear();
    userJwtFactory.genJwtEntry(expectedUser, actualResp.jwtToken());

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User actualUser = userRepository.findByEmail(expectedUser.getEmail()).orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });

    // Assert: Validate it is proper JWT token with correct signature and payload.
    Map<String, Object> expectedClaimMap = Maps.newHashMap();
    expectedClaimMap.put("iat", 1775822400L); // issued
    expectedClaimMap.put("exp", 1775844000L); // expires
    expectedClaimMap.put("sub", "test@example.com"); // user account email as subject
    jwtAssert.assertIt(actualResp.jwtToken(), expectedUser.getEmail(), expectedClaimMap);
  }

  //

  @Test
  public void errProlongWhenNotLogged() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Act: prolong user session... but there is no session.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/prolong"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        "Authentication is required to access this resource.",
        "/api/users/prolong",
        "https://api.general.org/errors/unauthorized",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
