package org.portfolio.userland.gcp.services;

import com.google.cloud.tasks.v2.QueueName;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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
      log.trace("Pre-warming Cloud Tasks gRPC connection...");
      String queuePath = QueueName.of(projectId, locationId, queueId).toString();
      // A simple "getQueue" call forces the networking layer to initialize.
      cloudTasksClient.getQueue(queuePath);
      log.trace("Cloud Tasks connection established.");
    } catch (Exception ex) {
      log.warn("Failed to pre-warm Cloud Tasks connection: {}", ex.getMessage());
    }
  }
}
