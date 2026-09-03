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
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.gcp.services.GcpCloudTaskService;
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
  private final GcpCloudTaskService gcpCloudTaskService;

  /**
   * Actually sends email. Available only to GCP Tasks.
   * @param emailReq Email request.
   * @return Response. 2xx means task success, and it will be removed from queue. 5xx means task failed, and it will be tried again.
   * It is possible to get 4xx from Spring validation on controller layer: failure that will not be tried again.
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
    boolean result = gcpCloudTaskService.processTaskEmailSend(emailReq);
    if (result) {
      // Returning 2xx tells GCP the task succeeded, and can be deleted from the queue.
      return new ResponseEntity<>(HttpStatus.OK);
    }

    // Returning 5xx tells GCP the task failed.
    // Cloud Tasks will automatically wait (using exponential backoff) and call this endpoint again later.
    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
