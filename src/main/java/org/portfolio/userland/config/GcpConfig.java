package org.portfolio.userland.config;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.CloudTasksSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.threeten.bp.Duration;

import java.io.IOException;

/**
 * GCP configuration class.
 */
@Configuration
public class GcpConfig {
  /**
   * Provides the <code>CloudTasksClient</code> bean. Used by {@link org.portfolio.userland.gcp.services.GcpTaskEnqueuer}.
   * In other environments there is no client bean at all - {@link org.portfolio.userland.gcp.services.NoopTaskEnqueuer}
   * is used instead (see {@link org.portfolio.userland.gcp.services.TaskEnqueuer}).
   * @return The configured <code>CloudTasksClient</code>.
   * @throws IOException If the client fails to initialize (e.g., missing credentials).
   */
  @Bean
  @Profile("gcp") // only on GCP Cloud
  public CloudTasksClient cloudTasksClient() throws IOException {
    // Configure aggressive retries for DEADLINE_EXCEEDED and UNAVAILABLE errors.
    RetrySettings retrySettings = RetrySettings.newBuilder()
        .setInitialRetryDelay(Duration.ofSeconds(1))
        .setRetryDelayMultiplier(2.0)
        .setMaxRetryDelay(Duration.ofSeconds(5))
        .setInitialRpcTimeout(Duration.ofSeconds(10))
        .setRpcTimeoutMultiplier(1.5)
        .setMaxRpcTimeout(Duration.ofSeconds(20))
        .setTotalTimeout(Duration.ofSeconds(30)) // Give the whole process more time to succeed. Note 30 seconds is allowed max.
        .build();

    // Apply the retry settings to the createTask operation.
    CloudTasksSettings settings = CloudTasksSettings.newBuilder()
        .applyToAllUnaryMethods(
            builder -> {
              builder.setRetrySettings(retrySettings);
              return null;
            }
        )
        .build();

    // This uses Application Default Credentials (ADC) automatically.
    // Locally, it uses `gcloud auth application-default login` credentials.
    // On GCP Cloud Run, it automatically uses Service Account.
    return CloudTasksClient.create(settings);
  }
}
