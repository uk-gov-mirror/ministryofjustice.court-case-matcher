package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import javax.validation.constraints.NotEmpty;

@Slf4j
@Component
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Profile("sqs-messaging")
public class SqsMessageReceiver implements MessageReceiver {

    @Autowired
    private final MessageProcessor messageProcessor;

    @Autowired
    private final TelemetryService telemetryService;

    @Autowired
    private final EventBus eventBus;

    @Autowired
    private final MessageParser<ExternalDocumentRequest> parser;

    @Value("${aws.sqs.queue_name}")
    private String queueName;

    @SqsListener(value = "${aws.sqs.queue_name}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void receive(@NotEmpty String message, @Header("MessageId") String messageId) {
        log.info("Received message from SQS queue {} with messageId: {}", queueName, messageId);
        telemetryService.trackSQSMessageEvent(messageId);
        process(message, messageId);
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
