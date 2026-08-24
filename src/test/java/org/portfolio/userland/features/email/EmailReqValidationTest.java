package org.portfolio.userland.features.email;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.email.dto.EmailReq;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests validation constraints declared on {@link EmailReq}. These run at REST boundary (GCP task callback) and
 * programmatically in <code>EmailService.queueEmail()</code>. Pure Bean Validation test - no Spring context needed.
 */
public class EmailReqValidationTest {
  private static Validator validator;

  /**
   * Prepares validator used by all tests.
   */
  @BeforeAll
  public static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  /**
   * Creates valid request mirroring internal registration-email flow (template-based).
   * @return Valid email request.
   */
  private EmailReq createValidTemplateRequest() {
    return new EmailReq(
        "resend",
        "pl",
        null,
        List.of("jan.kowalski@example.com"),
        List.of(),
        List.of(),
        "",
        "Welcome!",
        "user/registration",
        Map.of("username", "Jan Kowalski"),
        null);
  }

  /**
   * Verifies that real production shapes are all valid: template-based request, direct HTML request, and task
   * round-trip payload that contains BOTH template and rendered messageHtml.
   */
  @Test
  public void acceptsRealProductionShapes() {
    // Template-based request.
    assertThat(validator.validate(createValidTemplateRequest())).isEmpty();

    // Direct send with pre-rendered HTML only, empty lang, no provider (default will be used).
    EmailReq directHtml = createValidTemplateRequest().toBuilder()
        .provider(null).lang("").template(null).params(null).messageHtml("<p>Content</p>")
        .build();
    assertThat(validator.validate(directHtml)).isEmpty();

    // Task round-trip payload: process() fills messageHtml while keeping template.
    EmailReq taskPayload = createValidTemplateRequest().toBuilder()
        .messageHtml("<p>Rendered content</p>")
        .build();
    assertThat(validator.validate(taskPayload)).isEmpty();
  }

  /**
   * Verifies that missing or empty recipient list is rejected.
   */
  @Test
  public void rejectsMissingRecipients() {
    EmailReq nullRecipients = createValidTemplateRequest().toBuilder().recipients(null).build();
    EmailReq emptyRecipients = createValidTemplateRequest().toBuilder().recipients(List.of()).build();

    assertThat(validator.validate(nullRecipients)).anyMatch(v -> v.getPropertyPath().toString().equals("recipients"));
    assertThat(validator.validate(emptyRecipients)).anyMatch(v -> v.getPropertyPath().toString().equals("recipients"));
  }

  /**
   * Verifies that malformed email addresses are rejected - in recipients, CC, BCC and replyTo alike.
   */
  @Test
  public void rejectsMalformedAddresses() {
    EmailReq badRecipient = createValidTemplateRequest().toBuilder()
        .recipients(List.of("not-an-email")).build();
    EmailReq badCc = createValidTemplateRequest().toBuilder().recipientsCc(List.of("a@@b.com")).build();
    EmailReq badBcc = createValidTemplateRequest().toBuilder().recipientsBcc(List.of("no-at-sign.pl")).build();
    EmailReq badReplyTo = createValidTemplateRequest().toBuilder().replyTo("broken@").build();

    assertThat(validator.validate(badRecipient)).anyMatch(v -> v.getPropertyPath().toString().contains("recipients"));
    assertThat(validator.validate(badCc)).anyMatch(v -> v.getPropertyPath().toString().contains("recipientsCc"));
    assertThat(validator.validate(badBcc)).anyMatch(v -> v.getPropertyPath().toString().contains("recipientsBcc"));
    assertThat(validator.validate(badReplyTo)).anyMatch(v -> v.getPropertyPath().toString().equals("replyTo"));
  }

  /**
   * Verifies that too many recipients are rejected (list size limit).
   */
  @Test
  public void rejectsTooManyRecipients() {
    List<String> fiftyOne = IntStream.rangeClosed(1, 51).mapToObj(i -> "user" + i + "@example.com").toList();
    EmailReq tooMany = createValidTemplateRequest().toBuilder().recipients(fiftyOne).build();

    assertThat(validator.validate(tooMany)).anyMatch(v -> v.getMessage().contains("more than 50 recipients"));
  }

  /**
   * Verifies that blank or oversized subject is rejected.
   */
  @Test
  public void rejectsInvalidSubject() {
    EmailReq blankSubject = createValidTemplateRequest().toBuilder().subject("").build();
    EmailReq longSubject = createValidTemplateRequest().toBuilder().subject("x".repeat(201)).build();

    assertThat(validator.validate(blankSubject)).anyMatch(v -> v.getMessage().equals("Subject is required"));
    assertThat(validator.validate(longSubject)).anyMatch(v -> v.getMessage().contains("200 characters"));
  }

  /**
   * Verifies cross-field rule: request without template AND without messageHtml is rejected.
   */
  @Test
  public void rejectsMissingContentSource() {
    EmailReq noContent = createValidTemplateRequest().toBuilder().template(null).messageHtml(null).build();

    assertThat(validator.validate(noContent))
        .anyMatch(v -> v.getMessage().equals("Either template or messageHtml must be provided"));
  }

  /**
   * Verifies that oversized HTML content is rejected.
   */
  @Test
  public void rejectsOversizedMessageHtml() {
    EmailReq oversized = createValidTemplateRequest().toBuilder()
        .template(null)
        .messageHtml("<p>" + "x".repeat(90_001) + "</p>")
        .build();

    assertThat(validator.validate(oversized)).anyMatch(v -> v.getMessage().contains("90000 characters"));
  }

  /**
   * Verifies that invalid language code is rejected, while accepted formats ('', 'pl', 'en-US') pass.
   */
  @Test
  public void validatesLangFormat() {
    EmailReq badLang = createValidTemplateRequest().toBuilder().lang("polish").build();
    assertThat(validator.validate(badLang)).anyMatch(v -> v.getPropertyPath().toString().equals("lang"));

    for (String goodLang : new String[] {"", "pl", "en-US"}) {
      EmailReq good = createValidTemplateRequest().toBuilder().lang(goodLang).build();
      assertThat(validator.validate(good)).isEmpty();
    }
  }

  /**
   * Verifies that single-element helper getRecipients() joins correctly and handles empty input.
   */
  @Test
  public void recipientsJoinedHelperWorks() {
    assertThat(createValidTemplateRequest().getRecipients()).isEqualTo("jan.kowalski@example.com");
    assertThat(createValidTemplateRequest().toBuilder().recipients(Collections.emptyList()).build().getRecipients()).isEmpty();
  }
}
