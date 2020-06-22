package uk.gov.justice.probation.courtcasematcher.messaging;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageProcessor {

    @SuppressWarnings("UnstableApiUsage") // Not part of the final product
    private final EventBus eventBus;

    private final GatewayMessageParser parser;

    private final MatcherService matcherService;

    public MessageProcessor(GatewayMessageParser parser, EventBus eventBus, MatcherService matcherService) {
        super();
        this.parser = parser;
        this.eventBus = eventBus;
        this.matcherService = matcherService;
    }

    public void process(String message) {
        parse(message).ifPresent(this::process);
    }

    private Optional<List<Session>> parse(String message) {

        MessageType messageType;
        try {
            messageType = parser.parseMessage(message);
        } catch (IOException e) {
            log.error("Failed to parse message", e);
            eventBus.post(CourtCaseFailureEvent.builder().failureMessage(e.getMessage()).incomingMessage(message).build());
            return Optional.empty();
        }

        logMessageReceipt(messageType.getMessageHeader());

        List<Session> sessions = messageType.getMessageBody()
            .getGatewayOperationType()
            .getExternalDocumentRequest()
            .getDocumentWrapper()
            .getDocument()
            .stream()
            .flatMap(document -> document.getData().getJob().getSessions().stream())
            .collect(Collectors.toList());

        return Optional.of(sessions);
    }

    private void process(List<Session> sessions) {
        List<Case> cases = sessions
            .stream()
            .flatMap(session -> session.getBlocks().stream())
            .flatMap(block -> block.getCases().stream())
            .collect(Collectors.toList());

        log.info("Received {} cases", cases.size());
        cases.forEach(matcherService::match);
    }

    private void logMessageReceipt(MessageHeader messageHeader) {
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
    }
}
