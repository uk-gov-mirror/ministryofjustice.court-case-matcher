package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.google.common.eventbus.EventBus;
import javax.validation.ConstraintViolationException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DocumentWrapper;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

@Setter
@Service
@Slf4j
public class MessageProcessor {

    @Value("${case-feed-future-date-offset}")
    private int caseFeedFutureDateOffset;

    @SuppressWarnings("UnstableApiUsage") // Not part of the final product
    private final EventBus eventBus;

    private final GatewayMessageParser parser;

    private final MatcherService matcherService;

    @Autowired
    public MessageProcessor(GatewayMessageParser gatewayMessageParser,
                            EventBus eventBus,
                            MatcherService matcherService) {
        super();
        this.parser = gatewayMessageParser;
        this.eventBus = eventBus;
        this.matcherService = matcherService;
    }

    public void process(String message) {
        parse(message).ifPresent(this::process);
    }

    private Optional<MessageType> parse(String message) {

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

        return Optional.of(messageType);
    }

    private void process(MessageType messageType) {
        DocumentWrapper documentWrapper = messageType.getMessageBody().getGatewayOperationType().getExternalDocumentRequest().getDocumentWrapper();

        List<Session> sessions = documentWrapper.getDocument()
            .stream()
            .flatMap(document -> document.getData().getJob().getSessions().stream())
            .collect(Collectors.toList());

        List<CompletableFuture<Long>> futures = matchCases(sessions);

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
            MessageProcessorUtils.getCourtCodes(sessions).forEach(courtCode ->  {
                log.debug("Completed handling cases for court {}", courtCode);
            });
        });
    }

    private List<CompletableFuture<Long>> matchCases(List<Session> sessions) {

        List<CompletableFuture<Long>> allFutures = new ArrayList<>();
        sessions.forEach(session -> {
            List<Case> cases = session.getBlocks().stream()
                .flatMap(block -> block.getCases().stream())
                .collect(Collectors.toList());
            allFutures.add(CompletableFuture.supplyAsync(() -> matchCases(session, cases)));
        });
        return allFutures;
    }

    private Long matchCases(Session session, List<Case> cases) {
        log.debug("Matching {} cases for court {}, session {}", cases.size(), session.getCourtCode(), session.getId());
        cases.forEach(matcherService::match);
        return session.getId();
    }

    private void logMessageReceipt(MessageHeader messageHeader) {
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
    }

}
