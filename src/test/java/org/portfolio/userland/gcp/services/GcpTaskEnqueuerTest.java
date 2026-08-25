package org.portfolio.userland.gcp.services;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.OidcToken;
import com.google.cloud.tasks.v2.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.gcp.exceptions.GcpTaskEnqueueFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests enqueueing of tasks via GCP Cloud Tasks by {@link GcpTaskEnqueuer}: queue path resolution, OIDC token
 * setup and error handling when Cloud Tasks API call fails.
 */
public class GcpTaskEnqueuerTest {
  private CloudTasksClient cloudTasksClientMock;
  private GcpTaskEnqueuer gcpTaskEnqueuer;

  /**
   * Prepures enqueuer instance with mocked CloudTasksClient and configuration values.
   */
  @BeforeEach
  public void setUp() {
    cloudTasksClientMock = Mockito.mock(CloudTasksClient.class);
    gcpTaskEnqueuer = new GcpTaskEnqueuer(cloudTasksClientMock, JsonMapper.builder().findAndAddModules().build());
    ReflectionTestUtils.setField(gcpTaskEnqueuer, "serviceUrl", "https://test.example.com");
    ReflectionTestUtils.setField(gcpTaskEnqueuer, "serviceAccount", "test-sa");
    ReflectionTestUtils.setField(gcpTaskEnqueuer, "projectId", "test-project");
    ReflectionTestUtils.setField(gcpTaskEnqueuer, "locationId", "europe-central2");
  }

  /**
   * Verifies that failure of CloudTasksClient.createTask() results in domain-specific exception.
   */
  @Test
  public void enqueueThrowsDomainExceptionOnApiFailure() {
    // Arrange: make Cloud Tasks client fail.
    Mockito.doThrow(new ApiException(Mockito.mock(ApiException.class), Mockito.mock(StatusCode.class), false))
        .when(cloudTasksClientMock)
        .createTask(Mockito.anyString(), Mockito.any(Task.class));

    // Act & Assert: enqueueing must throw our domain-specific exception.
    Throwable thrown = catchThrowable(() -> gcpTaskEnqueuer.enqueue("test-queue", "/api/test", genEmailReq()));
    assertThat(thrown).isInstanceOf(GcpTaskEnqueueFailureException.class);
    GcpTaskEnqueueFailureException ex = (GcpTaskEnqueueFailureException) thrown;
    assertThat(ex.getTitle()).isEqualTo("Failed to enqueue GCP task.");
    assertThat(ex.getDetail()).contains("enqueue");
  }

  /**
   * Verifies that successful enqueueing does not throw any exception.
   */
  @Test
  public void enqueueSucceeds() {
    gcpTaskEnqueuer.enqueue("test-queue", "/api/test", genEmailReq());

    Mockito.verify(cloudTasksClientMock).createTask(Mockito.contains("queues/test-queue"), Mockito.any(Task.class));
  }

  /**
   * Verifies that enqueued task carries explicit OIDC audience matching the target endpoint URL. Receiving side
   * (JwtGcpTokenValidator) requires exactly this value, so both sides must stay in sync.
   */
  @Test
  public void enqueueSetsExpectedAudience() {
    ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

    gcpTaskEnqueuer.enqueue("test-queue", "/api/gcp/email/send", genEmailReq());

    Mockito.verify(cloudTasksClientMock).createTask(Mockito.anyString(), taskCaptor.capture());
    OidcToken oidcToken = taskCaptor.getValue().getHttpRequest().getOidcToken();
    assertThat(oidcToken.getAudience()).isEqualTo("https://test.example.com" + "/api/gcp/email/send");
    assertThat(oidcToken.getServiceAccountEmail()).isEqualTo("test-sa@test-project.iam.gserviceaccount.com");
  }

  /**
   * Generates simple email request used as task payload.
   * @return Email request.
   */
  private EmailReq genEmailReq() {
    return EmailReq.builder()
        .recipients(List.of("recipient@test.test"))
        .messageHtml("<p>Content</p>")
        .build();
  }
}
