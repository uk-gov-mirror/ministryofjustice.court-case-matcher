package uk.gov.justice.probation.courtcasematcher.service;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsServiceTest {

    private static final String QUEUE_NAME = "queue";

    @Mock
    private AmazonSQSAsync amazonSQSAsync;

    @InjectMocks
    private SqsService sqsService;

    @BeforeEach
    void beforeEach() {
        sqsService = new SqsService(QUEUE_NAME, amazonSQSAsync);
    }

    @DisplayName("When ask for correct queue then it is available")
    @Test
    void whenQueuePresent_thenAvailable() {

        GetQueueUrlResult res = new GetQueueUrlResult().withQueueUrl("URL");

        when(amazonSQSAsync.getQueueUrl(QUEUE_NAME)).thenReturn(res);

        assertThat(sqsService.isQueueAvailable()).isTrue();
    }

    @DisplayName("When AWS sends an error back then the queue is unavailable")
    @Test
    void givenAwsError_whenQueuePresent_thenUnavailable() {

        when(amazonSQSAsync.getQueueUrl(QUEUE_NAME)).thenThrow(new QueueDoesNotExistException("FAIL"));

        assertThat(sqsService.isQueueAvailable()).isFalse();
    }

    @DisplayName("When AWS returns empty queue URL")
    @Test
    void whenQueueUrlNotFound_thenUnavailable() {
        when(amazonSQSAsync.getQueueUrl(QUEUE_NAME)).thenReturn(new GetQueueUrlResult());

        assertThat(sqsService.isQueueAvailable()).isFalse();
    }

}
