package org.portfolio.userland.config;

import com.google.cloud.tasks.v2.CloudTasksClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
    // This uses Application Default Credentials (ADC) automatically.
    // Locally, it uses your `gcloud auth application-default login` credentials.
    // On GCP Cloud Run, it automatically uses your Service Account.
    return CloudTasksClient.create();
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
