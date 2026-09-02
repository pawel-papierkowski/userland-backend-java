package org.portfolio.userland.features.email.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;

import java.util.List;
import java.util.Map;

/**
 * Request for sending email. Contains all data needed to send arbitrary email using template or directly with content.
 * <p>Note: sender address is always overridden server-side with configured system sender - whatever caller provides
 * here is ignored (security measure against 'from' spoofing).</p>
 * @param provider  Use this provider. If empty/null, will use default provider.
 * @param lang Language. Example: "pl". If no language or unknown language, will fall back to "en".
 * @param sender Sender address. Ignored - server always uses configured system sender.
 * @param recipients Recipient address(es).
 * @param recipientsCc Recipient address(es), copy.
 * @param recipientsBcc Recipient address(es), hidden copy.
 * @param replyTo Address of reply. Can be null/empty.
 * @param subject Subject of email.
 * @param template Template to use.
 * @param params Parameters for template.
 * @param messageHtml HTML of email message. If null, system will try to compile template to fill this field.
 */
@Builder(toBuilder = true)
@Schema(description = "Request for sending an email.")
public record EmailReq(
    @Schema(description = "Use this provider. If empty/null, will use default provider.", example = "resend")
    String provider,

    @Pattern(regexp = "^(|[a-zA-Z]{2}(-[a-zA-Z]{2})?)$", message = "Language must be a 2-letter ISO code, optionally followed by region (example: 'pl' or 'pl-PL')")
    @Schema(description = "Language. If no language or unknown language, will fall back to 'en'.", example = "pl")
    String lang,

    @Schema(description = "Sender address. Ignored - server always overrides it with configured system sender.", example = "no-reply@example.com")
    String sender,

    @NotEmpty(message = "At least one recipient is required")
    @Size(max = 50, message = "Cannot send to more than 50 recipients")
    List<@Email(regexp = ValidConst.EMAIL_REGEXPR, message = "Must be a valid email address") @Valid String> recipients,
    @Schema(description = "Recipient address(es), copy.", example = "[\"john.doe@example.com\"]")
    List<@Email(regexp = ValidConst.EMAIL_REGEXPR, message = "Must be a valid email address") @Valid String> recipientsCc,
    List<@Email(regexp = ValidConst.EMAIL_REGEXPR, message = "Must be a valid email address") @Valid String> recipientsBcc,

    @Pattern(regexp = ValidConst.EMAIL_OR_EMPTY_REG_EXPR, message = "Must be a valid email address")
    @Schema(description = "Address of reply. Can be null/empty.", example = "support@example.com")
    String replyTo,

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    @Schema(description = "Subject of email.", example = "Welcome!")
    String subject,

    @Schema(description = "Template to use.", example = "user/registration")
    String template,
    @Schema(description = "Parameters for template.")
    Map<String, Object> params,

    @Size(max = 90000, message = "HTML content cannot exceed 90000 characters") // headroom under Cloud Tasks' 100KB task limit
    @Schema(description = "HTML of email message. If null, system will try to compile template to fill this field.")
    String messageHtml
) {
  /**
   * Cross-field invariant: email must have some content source - either a template to render, or pre-rendered HTML.
   * Note: task payloads contain BOTH (template is kept alongside rendered HTML), which is fine.
   * @return True if at least one content source is present.
   */
  @AssertTrue(message = "Either template or messageHtml must be provided")
  public boolean isContentPresent() {
    return template != null || messageHtml != null;
  }

  /**
   * Get list of recipients as single string.
   * @return Recipients separated by comma.
   */
  public String getRecipients() {
    if (recipients == null || recipients.isEmpty()) return "";
    return String.join(",", recipients);
  }
}
