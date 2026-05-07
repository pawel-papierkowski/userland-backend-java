package org.portfolio.userland.config;

import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.CloudTasksSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.threeten.bp.Duration;

import java.io.IOException;

@Configuration
public class GcpConfig {
  /**
   * Provides the CloudTasksClient bean.
   * Spring will automatically call close() on this bean when the application shuts down
   * because CloudTasksClient implements AutoCloseable.
   * @return The configured CloudTasksClient.
   * @throws IOException If the client fails to initialize (e.g., missing credentials).
   */
  @Bean
  @Profile("gcp") // only on GCP Cloud
  public CloudTasksClient cloudTasksClient() throws IOException {
    // Configure aggressive retries for DEADLINE_EXCEEDED and UNAVAILABLE errors.
    RetrySettings retrySettings = RetrySettings.newBuilder()
        .setInitialRetryDelay(Duration.ofSeconds(1))
        .setRetryDelayMultiplier(2.0)
        .setMaxRetryDelay(Duration.ofSeconds(10))
        .setInitialRpcTimeout(Duration.ofSeconds(30))
        .setRpcTimeoutMultiplier(2.0)
        .setMaxRpcTimeout(Duration.ofSeconds(60))
        .setTotalTimeout(Duration.ofSeconds(120)) // Give the whole process up to 60s to succeed
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
    // Locally, it uses your `gcloud auth application-default login` credentials.
    // On GCP Cloud Run, it automatically uses your Service Account.
    return CloudTasksClient.create(settings);
  }

  /**
   * Fake CloudTasksClient. Will not do anything, but app won't crash.
   * @return Fake CloudTasksClient.
   */
  @Bean
  @Profile("!gcp") // for local development, won't be used, but app would crash
  public CloudTasksClient fakeCloudTasksClient() {
    return null;
  }
}
