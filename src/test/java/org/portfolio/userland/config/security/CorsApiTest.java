package org.portfolio.userland.config.security;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

/**
 * Tests that CORS configuration defined in {@link org.portfolio.userland.config.security.SecurityConfig} works
 * correctly: allowed origins get proper CORS headers, disallowed origins do not.
 * <p>Uses the unsecured <code>/api/checks/alive</code> endpoint so no authentication is needed.</p>
 */
public class CorsApiTest extends BaseIntegrationTest {

  private static final String ALIVE_ENDPOINT = "/api/checks/alive";
  private static final String ALLOWED_ORIGIN = "http://localhost:5173";
  private static final String DISALLOWED_ORIGIN = "https://evil.example.com";

  // //////////////////////////////////////////////////////////////////////////
  // Preflight (OPTIONS)

  @Test
  void preflightFromAllowedOrigin_returnsCorsHeaders() throws Exception {
    MvcResult mvcResult = mockMvc.perform(options(ALIVE_ENDPOINT)
            .header("Origin", ALLOWED_ORIGIN)
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andReturn();

    assertThat(mvcResult.getResponse().getStatus())
        .as("Preflight should succeed")
        .isEqualTo(HttpStatus.OK.value());
    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Origin"))
        .as("Allowed origin should be reflected")
        .isEqualTo(ALLOWED_ORIGIN);
    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Methods"))
        .as("Allowed methods should be present")
        .isNotNull();
    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Headers"))
        .as("Allowed headers should be present")
        .isNotNull();
    assertThat(mvcResult.getResponse().getHeader("Access-Control-Max-Age"))
        .as("Max age should be 3600")
        .isEqualTo("3600");
    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Credentials"))
        .as("Credentials should be allowed")
        .isEqualTo("true");
  }

  @Test
  void preflightFromDisallowedOrigin_noCorsHeaders() throws Exception {
    MvcResult mvcResult = mockMvc.perform(options(ALIVE_ENDPOINT)
            .header("Origin", DISALLOWED_ORIGIN)
            .header("Access-Control-Request-Method", "GET"))
        .andReturn();

    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Origin"))
        .as("Disallowed origin must NOT get CORS headers")
        .isNull();
  }

  // //////////////////////////////////////////////////////////////////////////
  // Actual CORS requests

  @Test
  void actualRequestFromAllowedOrigin_returnsCorsHeaders() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get(ALIVE_ENDPOINT)
            .header("Origin", ALLOWED_ORIGIN))
        .andReturn();

    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Origin"))
        .as("Allowed origin should be reflected")
        .isEqualTo(ALLOWED_ORIGIN);
    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Credentials"))
        .as("Credentials should be allowed")
        .isEqualTo("true");
  }

  @Test
  void actualRequestFromDisallowedOrigin_noCorsHeaders() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get(ALIVE_ENDPOINT)
            .header("Origin", DISALLOWED_ORIGIN))
        .andReturn();

    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Origin"))
        .as("Disallowed origin must NOT get CORS headers")
        .isNull();
  }

  // //////////////////////////////////////////////////////////////////////////
  // Preflight with disallowed method

  @Test
  void preflightWithDisallowedMethod_noCorsHeaders() throws Exception {
    // "REPORT" is not in CorsConst.ALLOWED_METHODS.
    MvcResult mvcResult = mockMvc.perform(options(ALIVE_ENDPOINT)
            .header("Origin", ALLOWED_ORIGIN)
            .header("Access-Control-Request-Method", "REPORT"))
        .andReturn();

    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Origin"))
        .as("Disallowed method must NOT get CORS headers")
        .isNull();
  }

  // //////////////////////////////////////////////////////////////////////////
  // Preflight with disallowed header

  @Test
  void preflightWithDisallowedHeader_noCorsHeaders() throws Exception {
    // "X-Custom-Header" is not in CorsConst.ALLOWED_HEADERS.
    MvcResult mvcResult = mockMvc.perform(options(ALIVE_ENDPOINT)
            .header("Origin", ALLOWED_ORIGIN)
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "X-Custom-Header"))
        .andReturn();

    assertThat(mvcResult.getResponse().getHeader("Access-Control-Allow-Origin"))
        .as("Disallowed header must NOT get CORS headers")
        .isNull();
  }
}
