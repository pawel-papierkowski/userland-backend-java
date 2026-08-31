package org.portfolio.userland.system.auth;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for rate limiting. Enables rate limiting with tight limits
 * to verify behavior without long waits.
 * <p>Each test uses a unique IP via {@code X-Forwarded-For} header to get isolated buckets.
 * The rate limit filter runs before authentication, so we can test rate limiting on any endpoint
 * regardless of auth requirements — 401 means the request passed rate limiting, 429 means it was blocked.</p>
 */
@TestPropertySource(properties = {
    "app.rate-limit.active=true", // Activate rate limiting for this test file.
    // Tight limits for strict profile: 2 req/min
    "app.rate-limit.profiles.strict.limits[0].capacity=2",
    "app.rate-limit.profiles.strict.limits[0].refill-tokens=2",
    "app.rate-limit.profiles.strict.limits[0].refill-interval=1",
    // Tight limits for standard profile: 3 req/min burst, 5/hr sustained
    "app.rate-limit.profiles.standard.limits[0].capacity=3",
    "app.rate-limit.profiles.standard.limits[0].refill-tokens=3",
    "app.rate-limit.profiles.standard.limits[0].refill-interval=1",
    "app.rate-limit.profiles.standard.limits[1].capacity=5",
    "app.rate-limit.profiles.standard.limits[1].refill-tokens=5",
    "app.rate-limit.profiles.standard.limits[1].refill-interval=60"
})
class RateLimitIntegrationTest extends BaseIntegrationTest {
  @Test
  void rateLimitEnforced_whenRequestsExceedBurstCapacity() throws Exception {
    String ip = "rate-limit-test-1";

    // Act: Standard profile - burst capacity = 3. First 3 requests should pass rate limiting.
    MvcResult r1 = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();
    MvcResult r2 = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();
    MvcResult r3 = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();
    MvcResult r4 = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();

    // Assert: First 3 pass rate limiting but fail auth (401).
    assertThat(r1.getResponse().getStatus()).as("Request 1 passes rate limit").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(r2.getResponse().getStatus()).as("Request 2 passes rate limit").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(r3.getResponse().getStatus()).as("Request 3 passes rate limit").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    // Assert: 4th exceeds burst capacity, so it gets 429.
    assertThat(r4.getResponse().getStatus()).as("Request 4 is rate limited").isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
  }

  @Test
  void rateLimitProfileSeparateBuckets() throws Exception {
    String strictIp = "rate-limit-test-2-strict";
    String standardIp = "rate-limit-test-2-standard";

    // Act: Exhaust strict profile (capacity = 2) on /api/users/login.
    MvcResult login1 = mockMvc.perform(post("/api/users/login")
            .header("X-Forwarded-For", strictIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andReturn();
    MvcResult login2 = mockMvc.perform(post("/api/users/login")
            .header("X-Forwarded-For", strictIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andReturn();
    MvcResult login3 = mockMvc.perform(post("/api/users/login")
            .header("X-Forwarded-For", strictIp)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andReturn();

    // Assert: Strict profile - first 2 pass (400 for invalid body), 3rd is rate limited (429).
    assertThat(login1.getResponse().getStatus()).as("Login 1 passes rate limit")
        .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(login2.getResponse().getStatus()).as("Login 2 passes rate limit")
        .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(login3.getResponse().getStatus()).as("Login 3 is rate limited")
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

    // Act: Call endpoints with different ip.
    MvcResult view1 = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", standardIp)).andReturn();
    MvcResult view2 = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", standardIp)).andReturn();
    MvcResult view3 = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", standardIp)).andReturn();

    // Assert: Standard profile (capacity = 3) on /api/users/view should still work with separate IP.
    assertThat(view1.getResponse().getStatus()).as("View 1 passes rate limit").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(view2.getResponse().getStatus()).as("View 2 passes rate limit").isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(view3.getResponse().getStatus()).as("View 3 passes rate limit").isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  void excludedPathNotRateLimited() throws Exception {
    String ip = "rate-limit-test-3";

    for (int i = 0; i < 10; i++) {
      // Act: Send many requests.
      MvcResult result = mockMvc.perform(get("/api/checks/alive").header("X-Forwarded-For", ip)).andReturn();
      // Assert: /api/checks/alive is excluded from rate limiting.
      assertThat(result.getResponse().getStatus())
          .as("Request %d to excluded path should not be rate limited".formatted(i + 1))
          .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
  }

  @Test
  void retryAfterHeaderPresent_whenRateLimited() throws Exception {
    String ip = "rate-limit-test-4";

    // Act: Standard profile - burst capacity = 3. Exhaust it.
    mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();
    mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();
    mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();
    MvcResult rateLimited = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip)).andReturn();

    // Assert: Correct header is present for rate-limited response.
    assertThat(rateLimited.getResponse().getStatus())
        .as("Request should be rate limited")
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(rateLimited.getResponse().getHeader("Retry-After"))
        .as("Retry-After header should be present")
        .isNotNull();
  }

  @Test
  void differentIpsGetSeparateBuckets() throws Exception {
    // Both IPs hit standard profile (capacity = 3)
    String ip1 = "rate-limit-test-5-ip1";
    String ip2 = "rate-limit-test-5-ip2";

    // Act: Exhaust IP1's bucket
    mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip1)).andReturn();
    mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip1)).andReturn();
    mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip1)).andReturn();

    MvcResult ip1Result = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip1)).andReturn();
    MvcResult ip2Result = mockMvc.perform(get("/api/users/view").header("X-Forwarded-For", ip2)).andReturn();

    // Assert: IP2 should still have its own bucket
    assertThat(ip1Result.getResponse().getStatus())
        .as("IP1 should be rate limited")
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    assertThat(ip2Result.getResponse().getStatus())
        .as("IP2 should not be rate limited")
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }
}
