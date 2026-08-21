package org.portfolio.userland.gcp.services;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.gcp.exceptions.GcpTaskEnqueueFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests queuing of email tasks via GCP Cloud Tasks, focusing on error handling
 * when Cloud Tasks API call fails.
 */
public class GcpEmailServiceTest {
  private CloudTasksClient cloudTasksClientMock;
  private GcpEmailService gcpEmailService;

  /**
   * Prepares service instance with mocked CloudTasksClient and configuration values.
   */
  @BeforeEach
  public void setUp() {
    cloudTasksClientMock = Mockito.mock(CloudTasksClient.class);
    gcpEmailService = new GcpEmailService();
    ReflectionTestUtils.setField(gcpEmailService, "cloudTasksClient", cloudTasksClientMock);
    ReflectionTestUtils.setField(gcpEmailService, "serviceUrl", "https://test.example.com");
    ReflectionTestUtils.setField(gcpEmailService, "serviceAccount", "test-sa");
    ReflectionTestUtils.setField(gcpEmailService, "projectId", "test-project");
    ReflectionTestUtils.setField(gcpEmailService, "locationId", "europe-central2");
    ReflectionTestUtils.setField(gcpEmailService, "queueId", "test-queue");
  }

  /**
   * Verifies that failure of CloudTasksClient.createTask() results in domain-specific exception.
   */
  @Test
  public void queueEmailTaskThrowsDomainExceptionOnApiFailure() {
    // Arrange: make Cloud Tasks client fail.
    Mockito.doThrow(new ApiException(Mockito.mock(ApiException.class), Mockito.mock(StatusCode.class), false))
        .when(cloudTasksClientMock)
        .createTask(Mockito.anyString(), Mockito.any(Task.class));

    EmailReq emailReq = EmailReq.builder()
        .recipients(List.of("recipient@test.test"))
        .messageHtml("<p>Content</p>")
        .build();

    // Act & Assert: enqueueing must throw our domain-specific exception.
    Throwable thrown = catchThrowable(() -> gcpEmailService.queueEmailTask(emailReq));
    assertThat(thrown).isInstanceOf(GcpTaskEnqueueFailureException.class);
    GcpTaskEnqueueFailureException ex = (GcpTaskEnqueueFailureException) thrown;
    assertThat(ex.getTitle()).isEqualTo("Failed to enqueue GCP task.");
    assertThat(ex.getDetail()).contains("queueEmailTask");
  }

  /**
   * Verifies that successful enqueueing does not throw any exception.
   */
  @Test
  public void queueEmailTaskSucceeds() {
    EmailReq emailReq = EmailReq.builder()
        .recipients(List.of("recipient@test.test"))
        .messageHtml("<p>Content</p>")
        .build();

    gcpEmailService.queueEmailTask(emailReq);

    Mockito.verify(cloudTasksClientMock).createTask(Mockito.contains("queues/test-queue"), Mockito.any(Task.class));
  }
}
