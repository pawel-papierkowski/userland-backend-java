package org.portfolio.userland.gcp;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.config.security.JwtGcpTokenValidator;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.exceptions.EmailSendFailureException;
import org.portfolio.userland.features.email.services.EmailService;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.portfolio.userland.test.helpers.gcp.GcpOidcTokenHelper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration tests for {@link org.portfolio.userland.gcp.controllers.GcpController} covering the full
 * GCP OIDC auth chain end-to-end.
 * <p>Tests exercise the real GCP security filter chain ({@code gcpInternalSecurityFilterChain}) with a local
 * RSA key-based decoder that replaces the Google JWKS decoder. The real {@link JwtGcpTokenValidator} is still
 * in the chain, verifying audience and service account identity.</p>
 * <p>Issuer validation is intentionally skipped (already covered by unit tests).</p>
 */
@Import(GcpApiTest.TestConfig.class)
public class GcpApiTest extends BaseIntegrationTest {
  private static final String ENDPOINT = "/api/gcp/email/send";

  /** Prevents real email sending when GcpService delegates to EmailService. */
  @MockitoBean
  protected EmailService emailService;

  // //////////////////////////////////////////////////////////////////////////
  // Test configuration: override gcpJwtDecoder with local RSA key-based decoder

  @TestConfiguration
  static class TestConfig {
    @Primary
    @Bean("gcpJwtDecoder")
    JwtDecoder gcpJwtDecoder() {
      return GcpOidcTokenHelper.createTestDecoder();
    }
  }

  // //////////////////////////////////////////////////////////////////////////
  // OIDC Auth Chain tests

  @Test
  void noToken_returns401() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.UNAUTHORIZED);
    verify(emailService, never()).sendEmail(any());
  }

  @Test
  void malformedToken_returns401() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.malformedToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.UNAUTHORIZED);
    verify(emailService, never()).sendEmail(any());
  }

  @Test
  void wrongAudience_returns401() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.wrongAudienceToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.UNAUTHORIZED);
    verify(emailService, never()).sendEmail(any());
  }

  @Test
  void missingAudience_returns401() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.missingAudienceToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.UNAUTHORIZED);
    verify(emailService, never()).sendEmail(any());
  }

  @Test
  void wrongIdentity_returns401() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.wrongIdentityToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.UNAUTHORIZED);
    verify(emailService, never()).sendEmail(any());
  }

  @Test
  void missingIdentity_returns401() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.missingIdentityToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.UNAUTHORIZED);
    verify(emailService, never()).sendEmail(any());
  }

  @Test
  void expiredToken_returns401() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.expiredToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.UNAUTHORIZED);
    verify(emailService, never()).sendEmail(any());
  }

  // //////////////////////////////////////////////////////////////////////////
  // Controller behavior tests (valid token, different outcomes)

  @Test
  void validToken_success_returns200() throws Exception {
    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.validToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.OK);
    verify(emailService).sendEmail(any(EmailReq.class));
  }

  @Test
  void validToken_failure_returns500() throws Exception {
    doThrow(new EmailSendFailureException("test", new RuntimeException(" SMTP error")))
        .when(emailService).sendEmail(any());

    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.validToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(validEmailJson()))
        .andReturn();

    assertThat(mvcResult, HttpStatus.INTERNAL_SERVER_ERROR);
    verify(emailService).sendEmail(any(EmailReq.class));
  }

  @Test
  void validToken_invalidBody_returns400() throws Exception {
    String invalidJson = """
        {
          "recipients": [],
          "messageHtml": "<p>Test</p>"
        }
        """;

    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.validToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andReturn();

    assertThat(mvcResult, HttpStatus.BAD_REQUEST);
    verify(emailService, never()).sendEmail(any());
  }

  @Test
  void validToken_noContentSource_returns400() throws Exception {
    String noContentJson = """
        {
          "recipients": ["test@example.com"],
          "subject": "Test"
        }
        """;

    MvcResult mvcResult = mockMvc.perform(post(ENDPOINT)
            .header("Authorization", "Bearer " + GcpOidcTokenHelper.validToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(noContentJson))
        .andReturn();

    assertThat(mvcResult, HttpStatus.BAD_REQUEST);
    verify(emailService, never()).sendEmail(any());
  }

  // //////////////////////////////////////////////////////////////////////////
  // Helpers

  /**
   * Asserts that the response has the expected HTTP status.
   * @param mvcResult MVC result.
   * @param expectedStatus Expected HTTP status.
   */
  private void assertThat(MvcResult mvcResult, HttpStatus expectedStatus) {
    org.assertj.core.api.Assertions.assertThat(mvcResult.getResponse().getStatus())
        .as("HTTP status should be %s", expectedStatus)
        .isEqualTo(expectedStatus.value());
  }

  /**
   * Returns a valid email request JSON body.
   * @return JSON string.
   */
  private String validEmailJson() {
    return """
        {
          "recipients": ["test@example.com"],
          "subject": "Test Email",
          "messageHtml": "<p>Hello World</p>"
        }
        """;
  }
}
