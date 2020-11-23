package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@Slf4j
@Component
@ConditionalOnBean(name = "mqJmsListenerContainerFactory")
@RequiredArgsConstructor
public class MqMessageReceiver implements MessageReceiver {

    private final MessageProcessor messageProcessor;

    private final TelemetryService telemetryService;

    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;

    private final GatewayMessageParser<MessageType> parser;

    @Value("${messaging.activemq.queueName}")
    private String queueName;

    @JmsListener(destination = "${messaging.activemq.queueName}")
    public void receive(String message) {
        log.info("Received message from JMS, queue name {}", queueName);
        telemetryService.trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
        process(message);
    }

    @Override
    public ExternalDocumentRequest parse(String message) throws JsonProcessingException {

        MessageType messageType = parser.parseMessage(message, MessageType.class);
        MessageHeader messageHeader = messageType.getMessageHeader();
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
        return messageType.getMessageBody().getGatewayOperationType().getExternalDocumentRequest();
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
