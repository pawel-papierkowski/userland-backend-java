package org.portfolio.userland.gcp.services;

import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.tasks.v2.QueueName;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.EmailService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * General GCP service.
 */
@Service
@RequiredArgsConstructor
@Lazy(false) // Overrides global lazy-initialization: true, so init() is called as soon as possible.
@Slf4j
public class GcpService extends BaseGcpService {
  private final EmailService emailService;

  /**
   * Make a dummy, lightweight network call to force DNS resolution and TCP/TLS handshakes BEFORE any user traffic hits
   * the server.
   */
  @PostConstruct
  public void init() {
    if (cloudTasksClient == null) return;
    try {
      debugGetCurrentAccount();
      String queuePath = QueueName.of(projectId, locationId, queueId).toString();
      log.trace("Pre-warming Cloud Tasks gRPC connection. queuePath: {}", queuePath);
      // A simple "getQueue" call forces the networking layer to initialize.
      cloudTasksClient.getQueue(queuePath);
      log.trace("Cloud Tasks connection established.");
    } catch (Exception ex) {
      log.warn("Failed to pre-warm Cloud Tasks connection: {}", ex.getMessage());
    }
  }

  /**
   * Prints to console currently used GCP account.
   * @throws IOException When something goes wrong.
   */
  public void debugGetCurrentAccount() throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
    if (credentials instanceof ServiceAccountCredentials) {
      String activeEmail = ((ServiceAccountCredentials) credentials).getAccount();
      log.debug("GCP currently running as Service Account: {}", activeEmail);
    } else if (credentials instanceof UserCredentials) {
      String clientId = ((UserCredentials) credentials).getClientId();
      log.debug("GCP currently running as User Account: {}", clientId);
    } else if (credentials instanceof ComputeEngineCredentials) {
      String accountId = ((ComputeEngineCredentials) credentials).getAccount();
      log.debug("GCP currently running as compute engine: {}", accountId);
    } else {
      log.debug("GCP currently running as an unknown credential type. Type of credential: {}.",
          credentials.getClass().getName());
    }
  }

  //

  /**
   *
   * Actually sends email. Available only to GCP Tasks.
   * <p>Security note: payload comes from our own queue and is already sanitized at enqueue time, but we apply
   * defense-in-depth restrictions here too (see {@link #sanitizeTaskPayload(EmailReq)}). Sender address is
   * additionally overridden with system value inside <code>EmailService.sendEmail()</code>.</p>
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
