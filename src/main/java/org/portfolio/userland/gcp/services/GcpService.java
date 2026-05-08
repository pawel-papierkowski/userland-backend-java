package org.portfolio.userland.gcp.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.tasks.v2.QueueName;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * General GCP service.
 */
@Service
@Lazy(false) // Overrides global lazy-initialization: true
@Slf4j
public class GcpService extends BaseGcpService {
  @PostConstruct
  public void init() {
    if (cloudTasksClient == null) return;
    // Make a dummy, lightweight network call to force DNS resolution and TCP/TLS handshakes
    // BEFORE any user traffic hits the server.
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
    } else {
      log.debug("GCP currently running as an compute engine default.");
    }
  }
}
