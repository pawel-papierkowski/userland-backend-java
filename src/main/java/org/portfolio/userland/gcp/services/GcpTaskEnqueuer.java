package org.portfolio.userland.gcp.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ApiException;
import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.tasks.v2.*;
import com.google.protobuf.ByteString;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.gcp.exceptions.GcpTaskEnqueueFailureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Real implementation of {@link TaskEnqueuer} backed by GCP Cloud Tasks. Active only on 'gcp' profile.
 */
@Service
@Profile("gcp")
@RequiredArgsConstructor
@Lazy(false) // Overrides global lazy-initialization: true, so init() is called as soon as possible.
@Slf4j
public class GcpTaskEnqueuer implements TaskEnqueuer {
  private final CloudTasksClient cloudTasksClient;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Value("${app.gcp.general.url}")
  private String serviceUrl;
  @Value("${app.gcp.general.service-account}")
  private String serviceAccount;
  @Value("${app.gcp.general.project}")
  private String projectId;
  @Value("${app.gcp.general.location}")
  private String locationId;
  /** Queue used for connection pre-warm-up at startup (currently the only queue in the system). */
  @Value("${app.gcp.email.queue}")
  private String warmUpQueueId;

  /**
   * Make a dummy, lightweight network call to force DNS resolution and TCP/TLS handshakes BEFORE any user traffic hits
   * the server.
   */
  @PostConstruct
  public void init() {
    try {
      debugGetCurrentAccount();
      String queuePath = QueueName.of(projectId, locationId, warmUpQueueId).toString();
      log.trace("Pre-warming Cloud Tasks gRPC connection. queuePath: {}", queuePath);
      // A simple "getQueue" call forces the networking layer to initialize.
      cloudTasksClient.getQueue(queuePath);
      log.trace("Cloud Tasks connection established.");
    } catch (IOException | ApiException ex) {
      // Expected failure modes of the pre-warm call (credential resolution and gRPC/network/permission errors).
      // Pre-warm is best-effort - failure here degrades gracefully, it must not prevent application startup.
      // Anything else is a genuine bug and must propagate, so a broken deployment fails fast on Cloud Run.
      log.warn("Failed to pre-warm Cloud Tasks connection: {}", ex.getMessage());
      log.debug("Cloud Tasks pre-warm failure details.", ex);
    }
  }

  @Override
  public void enqueue(String queueId, String endpointPath, Object payload) {
    try {
      String queuePath = QueueName.of(projectId, locationId, queueId).toString();
      String jsonPayload = objectMapper.writeValueAsString(payload);
      String fullServiceAccountEmail = serviceAccount+"@"+projectId+".iam.gserviceaccount.com";
      String targetUrl = serviceUrl + endpointPath;

      log.trace("enqueue(): Payload:\n{}", jsonPayload);

      // Build the HTTP request that GCP will make back to your app.
      HttpRequest httpRequest = HttpRequest.newBuilder()
          .setUrl(targetUrl)
          .setHttpMethod(HttpMethod.POST)
          .putHeaders("Content-Type", "application/json")
          .setBody(ByteString.copyFromUtf8(jsonPayload))
          // Secure it via OIDC so only Cloud Tasks can call this endpoint.
          // Audience is set explicitly (instead of relying on the URL default) so it exactly matches what
          // JwtGcpTokenValidator expects on the receiving side.
          .setOidcToken(OidcToken.newBuilder()
              .setServiceAccountEmail(fullServiceAccountEmail)
              .setAudience(targetUrl)
              .build())
          .build();

      Task task = Task.newBuilder()
          .setHttpRequest(httpRequest)
          .build();

      // Send to GCP
      cloudTasksClient.createTask(queuePath, task);
    } catch (JsonProcessingException | ApiException ex) {
      throw new GcpTaskEnqueueFailureException("enqueue", ex);
    }
  }

  /**
   * Prints to console currently used GCP account.
   * @throws IOException When something goes wrong.
   */
  public void debugGetCurrentAccount() throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
    switch (credentials) {
      case ServiceAccountCredentials serviceAccountCredentials -> {
        String activeEmail = serviceAccountCredentials.getAccount();
        log.debug("GCP currently running as Service Account: {}", activeEmail);
      }
      case UserCredentials userCredentials -> {
        String clientId = userCredentials.getClientId();
        log.debug("GCP currently running as User Account: {}", clientId);
      }
      case ComputeEngineCredentials computeEngineCredentials -> {
        String accountId = computeEngineCredentials.getAccount();
        log.debug("GCP currently running as compute engine: {}", accountId);
      }
      default -> log.debug("GCP currently running as an unknown credential type. Type of credential: {}.",
          credentials.getClass().getName());
    }
  }
}
