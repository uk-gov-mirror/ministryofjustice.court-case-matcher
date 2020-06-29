package uk.gov.justice.probation.courtcasematcher.messaging;

import com.google.common.eventbus.EventBus;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

@Service
@Slf4j
public class MessageProcessor {

    @SuppressWarnings("UnstableApiUsage") // Not part of the final product
    private final EventBus eventBus;

    private final GatewayMessageParser parser;

    private final MatcherService matcherService;

    @Autowired
    public MessageProcessor(GatewayMessageParser gatewayMessageParser, EventBus eventBus, MatcherService matcherService) {
        super();
        this.parser = gatewayMessageParser;
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
        }
        catch (ConstraintViolationException | IOException ex) {
            log.error("Failed to parse and validate message", ex);
            CourtCaseFailureEvent.CourtCaseFailureEventBuilder builder = CourtCaseFailureEvent.builder()
                .failureMessage(ex.getMessage())
                .incomingMessage(message);
            if (ex instanceof ConstraintViolationException) {
                builder.violations(((ConstraintViolationException)ex).getConstraintViolations());
            }
            eventBus.post(builder.build());
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
