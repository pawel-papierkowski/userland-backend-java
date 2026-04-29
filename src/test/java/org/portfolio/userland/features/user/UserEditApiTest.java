package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for editing of user account.
 */
public class UserEditApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  public void fullEditUser() throws Exception {
    // Fully edit user.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user.
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq("Jasiu", "#eWp@s5w0rD", "pl");

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("Jasiu");
    expectedUser.setLang("pl");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EDIT, "username, password, lang");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      assertThat(actualUser.getPassword()).as("Password hash should be different").isNotEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserOneField() throws Exception {
    // Edit user: change only single field.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user.
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq("Robert", null, null);

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("Robert");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EDIT, "username");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      assertThat(actualUser.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserPasswordOnly() throws Exception {
    // Edit user: change only password.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user.
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq(null, "#eWp@s5w0rD", null);

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EDIT, "password");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      assertThat(actualUser.getPassword()).as("Password hash should be different").isNotEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserNoFields() throws Exception {
    // Edit user: no fields changed.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user.
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq(null, null, null);

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      assertThat(actualUser.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }
}
