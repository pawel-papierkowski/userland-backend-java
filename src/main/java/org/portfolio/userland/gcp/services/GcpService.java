package org.portfolio.userland.gcp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.EmailService;
import org.springframework.stereotype.Service;

/**
 * General GCP service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GcpService {
  private final EmailService emailService;

  /**
   * Actually sends email. Available only to GCP Tasks.
   * <p>Security note: payload comes from our own queue and will be sanitized here. We apply
   * defense-in-depth restrictions here (see {@link #sanitizeTaskPayload(EmailReq)}). Sender address is
   * overridden with system value inside <code>EmailService.sendEmail()</code>.</p>
   * <p>Note on validation: <code>@Valid</code> failures at the controller return 400, and any non-5xx response makes
   * Cloud Tasks delete the task silently. Since payloads are produced by us, a 400 here means producer/consumer
   * drift (e.g. deploy skew), not transient failure - dropping the task is the correct behavior.</p>
   * @param emailReq Email request.
   * @return True if task succeeded, otherwise false.
   */
  public boolean processTaskEmailSend(EmailReq emailReq) {
    emailReq = sanitizeTaskPayload(emailReq);
    log.trace("processTaskEmailSend(): Will try to send email to '{}'. Template: '{}'.",
        emailReq.getRecipients(), emailReq.template());

    try {
      emailService.sendEmail(emailReq);
      return true;
    } catch (GeneralException ex) {
      // Note: we must catch ALL domain exceptions here (even ones with 4xx status) - any non-5xx response would make
      // Cloud Tasks delete the task and the email would be silently lost.
      // Truly unexpected exceptions are not caught on purpose:
      // GlobalExceptionHandler converts them into 500 anyway, which preserves retry semantics too.
      log.error("Exception thrown!", ex);
      return false;
    }
  }

  /**
   * Sanitizes payload received from Cloud Tasks before sending.
   * <p>Provider selection is stripped - tasks must always use default provider. Note <code>messageHtml</code> is
   * intentionally kept: it was rendered by us from a system template at enqueue time (see
   * <code>EmailService.process()</code>) and re-rendering or dropping it would break delivery.</p>
   * @param emailReq Email request from task payload.
   * @return Sanitized copy of the request.
   */
  private EmailReq sanitizeTaskPayload(EmailReq emailReq) {
    return emailReq.toBuilder().provider(null).build();
  }
}
