package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@Slf4j
@Service
@ConditionalOnProperty(value="messaging.activemq.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MqMessageReceiver implements MessageReceiver {

    private static final String CP_QUEUE = "CP_OutboundQueue";

    private final MessageProcessor messageProcessor;

    private final TelemetryService telemetryService;

    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;

    private final MessageParser<MessageType> parser;

    @JmsListener(destination = CP_QUEUE)
    public void receive(String message) {
        log.info("Received message from JMS, queue name {}", CP_QUEUE);
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
