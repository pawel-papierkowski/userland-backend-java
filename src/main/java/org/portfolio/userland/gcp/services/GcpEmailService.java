package org.portfolio.userland.gcp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.tasks.v2.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.email.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Uses GCP Cloud Tasks to queue emails.
 * <p>Note: This code is not used at all in local environment. You are responsible for checking if <code>GcpEmailService</code>
 * can be called. Example:</p>
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
@Slf4j
public class GcpEmailService {
  /** CloudTasksClient is not injected in local development environment, as GCP-related stuff is not used. */
  @Autowired(required = false)
  private CloudTasksClient cloudTasksClient;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Value("${app.gcp.general.url}")
  private String serviceUrl;
  @Value("${app.gcp.general.service-account}")
  private String serviceAccount;

  @Value("${app.gcp.general.project}")
  private String projectId;
  @Value("${app.gcp.general.location}")
  private String locationId;
  @Value("${app.gcp.email.queue}")
  private String queueId;

  /**
   * Queues email task for GCP Tasks.
   * @param emailReq Email request.
   */
  public void queueEmailTask(EmailReq emailReq) {
    if (cloudTasksClient == null) throw new IllegalStateException("CloudTasksClient is null. Misconfigured/wrong environment?");

    try {
      String queuePath = QueueName.of(projectId, locationId, queueId).toString();
      String jsonPayload = objectMapper.writeValueAsString(emailReq);
      String fullServiceAccountEmail = serviceAccount+"@"+projectId+".iam.gserviceaccount.com";

      log.trace("queueEmailTask(): Email to '{}' (template '{}') is queued. queuePath: '{}', fullServiceAccountEmail: '{}'",
          emailReq.getRecipients(), emailReq.template(), queuePath, fullServiceAccountEmail);

      // Build the HTTP request that GCP will make back to your app.
      HttpRequest httpRequest = HttpRequest.newBuilder()
          .setUrl(serviceUrl + "/api/gcp/email/send")
          .setHttpMethod(HttpMethod.POST)
          .putHeaders("Content-Type", "application/json")
          .setBody(ByteString.copyFromUtf8(jsonPayload))
          // Secure it via OIDC so only Cloud Tasks can call this endpoint.
          .setOidcToken(OidcToken.newBuilder().setServiceAccountEmail(fullServiceAccountEmail).build())
          .build();

      Task task = Task.newBuilder()
          .setHttpRequest(httpRequest)
          .build();

      // Send to GCP
      cloudTasksClient.createTask(queuePath, task);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
