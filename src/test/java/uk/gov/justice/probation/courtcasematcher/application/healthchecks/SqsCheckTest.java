package uk.gov.justice.probation.courtcasematcher.application.healthchecks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;
import uk.gov.justice.probation.courtcasematcher.service.SqsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsCheckTest {

    @Mock
    private SqsService sqsService;

    @InjectMocks
    private SqsCheck sqsCheck;

    @Test
    void whenQueueAvailable_thenUp() {
        when(sqsService.isQueueAvailable()).thenReturn(Boolean.TRUE);

        assertThat(sqsCheck.health().block().getStatus()).isSameAs(Status.UP);
    }

    @Test
    void whenQueueUnavailable_thenDown() {
        when(sqsService.isQueueAvailable()).thenReturn(Boolean.FALSE);

        assertThat(sqsCheck.health().block().getStatus()).isSameAs(Status.DOWN);
    }

}
