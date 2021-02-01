package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import javax.validation.constraints.NotEmpty;

@Slf4j
@Service
@ConditionalOnProperty(value="messaging.sqs.enabled", havingValue = "true")
public class SqsMessageReceiver implements MessageReceiver {

    @Autowired
    private  MessageProcessor messageProcessor;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private MessageParser<ExternalDocumentRequest> parser;

    @Value("${aws_sqs_queue_name}")
    private String queueName;

    @SqsListener(value = "${aws_sqs_queue_name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receive(@NotEmpty String message, @Header("MessageId") String messageId) {
        log.info("Received message from SQS queue {} with messageId: {}", queueName, messageId);
        telemetryService.trackSQSMessageEvent(messageId);
        process(message);
    }

    public ExternalDocumentRequest parse(String message) throws JsonProcessingException {
        return parser.parseMessage(message, ExternalDocumentRequest.class);
    }

    @Override
    public MessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
