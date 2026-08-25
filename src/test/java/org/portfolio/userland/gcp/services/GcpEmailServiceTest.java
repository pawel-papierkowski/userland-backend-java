package org.portfolio.userland.gcp.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.gcp.constants.GcpConst;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link GcpEmailService} correctly delegates to {@link TaskEnqueuer}.
 */
public class GcpEmailServiceTest {
  private TaskEnqueuer taskEnqueuerMock;
  private GcpEmailService gcpEmailService;

  /**
   * Prepares service instance with mocked task enqueuer and configuration values.
   */
  @BeforeEach
  public void setUp() {
    taskEnqueuerMock = Mockito.mock(TaskEnqueuer.class);
    gcpEmailService = new GcpEmailService(taskEnqueuerMock);
    ReflectionTestUtils.setField(gcpEmailService, "queueId", "test-queue");
  }

  /**
   * Verifies that email request is delegated to the task enqueuer with correct queue and endpoint path.
   */
  @Test
  public void queueEmailTaskDelegatesToTaskEnqueuer() {
    EmailReq emailReq = EmailReq.builder()
        .recipients(List.of("recipient@test.test"))
        .messageHtml("<p>Content</p>")
        .build();

    gcpEmailService.queueEmailTask(emailReq);

    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(taskEnqueuerMock).enqueue(Mockito.eq("test-queue"), Mockito.eq(GcpConst.EMAIL_SEND_ENDPOINT_PATH), payloadCaptor.capture());
    assertThat(payloadCaptor.getValue()).isSameAs(emailReq);
  }
}
