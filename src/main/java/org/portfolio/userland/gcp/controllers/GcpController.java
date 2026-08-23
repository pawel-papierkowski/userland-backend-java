package org.portfolio.userland.gcp.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for GCP. All endpoints here are secured via OIDC token in <code>SecurityConfig</code>.
 */
@RestController
@RequestMapping("/api/gcp")
@RequiredArgsConstructor
@Tag(name = "GCP", description = "Endpoints for GCP.")
@Slf4j
public class GcpController {
  private final EmailService emailService;

  /**
   * Actually sends email. Available only to GCP Tasks.
   * @param emailReq Email request.
   * @return Response.
   */
  @PostMapping(value = "/email/send", produces = "application/json")
  @Operation(summary = "Send email", description = "Actually sends email. Available only to GCP Tasks.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Email sent successfully.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid Google-signed OIDC token.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Sending email failed.",
          content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<Void> processTaskEmailSend(@Valid @RequestBody EmailReq emailReq) {
    log.trace("sendEmail(): Will try to send email to '{}'. Template: '{}'.",
        emailReq.getRecipients(), emailReq.template());

    try {
      // Returning 2xx tells GCP the task succeeded, and can be deleted from the queue.
      emailService.sendEmail(emailReq);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (GeneralException ex) {
      log.error("Exception thrown!", ex);
      // Returning 5xx tells GCP the task failed.
      // Cloud Tasks will automatically wait (using exponential backoff) and call this endpoint again later.
      // Note: we must catch ALL domain exceptions here (even ones with 4xx status) - any non-5xx response would make
      // Cloud Tasks delete the task and the email would be silently lost. Truly unexpected exceptions are not caught
      // on purpose: GlobalExceptionHandler converts them into 500 anyway, which preserves retry semantics too.
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
