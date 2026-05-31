package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.services.standard.*;
import org.portfolio.userland.system.auth.details.CustomUserDetailsService;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.system.auth.perm.PermissionService;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.test.base.BaseWebTest;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tests only web layer of user admin handling.
 */
public class UserAdminWebTest extends BaseWebTest {
  // We mock services present on UserAdminController because we only care about testing the Controller's @Valid rules.
  @MockitoBean
  private UserRegisterService userRegisterService;
  @MockitoBean
  private UserViewService userViewService;
  @MockitoBean
  private UserEditService userEditService;
  @MockitoBean
  private UserEmailService userEmailService;
  @MockitoBean
  private UserPasswordService userPasswordService;
  @MockitoBean
  private UserDeleteService userDeleteService;
  //@MockitoBean
  //private UserTableService userTableService;

  // Other needed mocks.
  @MockitoBean
  private ConfigService configService;
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;
  @MockitoBean
  private PermissionService permissionService;

  @Test
  public void viewUserDataWhenWrongId() throws Exception {
    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/users/z"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "Not Found",
        "No static resource api/admin/users/z.",
        "/api/admin/users/z",
        null,
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
