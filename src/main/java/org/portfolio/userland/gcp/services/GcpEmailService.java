package org.portfolio.userland.gcp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.EmailService;
import org.portfolio.userland.gcp.constants.GcpConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Uses GCP Cloud Tasks to queue emails. Actual enqueueing is delegated to {@link TaskEnqueuer}, which is
 * environment-aware (real on GCP, no-op locally), so this service is safe to call in any environment.
 * <p>Note: caller is responsible for deciding whether emails should actually be queued via Cloud Tasks or sent
 * synchronously. Example:</p>
 * <pre>
 *   &#064;Value("${app.gcp.email.task}")
 *   private Boolean canEmailTask;
 *   ...
 *   public void queueEmail(EmailReq emailReq) {
 *     if (canEmailTask) gcpEmailService.queueEmailTask(emailReq);
 *     else sendEmail(emailReq);
 *   }
 * </pre>
 * @see EmailService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GcpEmailService {
  private final TaskEnqueuer taskEnqueuer;

  /** Queue that holds email tasks. */
  @Value("${app.gcp.email.queue}")
  private String queueId;

  /**
   * Queues email task for GCP Tasks.
   * @param emailReq Email request.
   */
  public void queueEmailTask(EmailReq emailReq) {
    log.trace("queueEmailTask(): Email to '{}' (template '{}') is queued.",
        emailReq.getRecipients(), emailReq.template());
    taskEnqueuer.enqueue(queueId, GcpConst.EMAIL_SEND_ENDPOINT_PATH, emailReq);
  }
}
