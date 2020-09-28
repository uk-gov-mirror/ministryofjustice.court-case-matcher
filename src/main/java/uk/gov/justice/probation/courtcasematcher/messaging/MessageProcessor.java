package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.common.eventbus.EventBus;
import javax.validation.ConstraintViolationException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseMatchEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseUpdateEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DocumentWrapper;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@Setter
@Service
@Slf4j
public class MessageProcessor {

    @Value("${case-feed-future-date-offset}")
    private int caseFeedFutureDateOffset;

    @SuppressWarnings("UnstableApiUsage") // Not part of the final product
    private final EventBus eventBus;

    private final GatewayMessageParser parser;

    private final TelemetryService telemetryService;

    private final CourtCaseService courtCaseService;

    @Autowired
    public MessageProcessor(GatewayMessageParser gatewayMessageParser,
                            EventBus eventBus,
                            TelemetryService telemetryService,
                            CourtCaseService courtCaseService) {
        super();
        this.parser = gatewayMessageParser;
        this.eventBus = eventBus;
        this.telemetryService = telemetryService;
        this.courtCaseService = courtCaseService;
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
            .map(this::trackCourtListReceipt)
            .flatMap(document -> document.getData().getJob().getSessions().stream())
            .collect(Collectors.toList());

        saveCases(sessions);
    }

    private void saveCases(List<Session> sessions) {
        sessions.forEach(session -> {
            log.debug("Starting to process cases in session court {}, room {}, date {}",
                session.getCourtCode(), session.getCourtRoom(), session.getDateOfHearing());

            List<String> cases = session.getBlocks().stream()
                .flatMap(block -> block.getCases().stream())
                .map(this::saveCase)
                .collect(Collectors.toList());
            log.debug("Completed {} cases for {}, {}, {}", cases.size(), session.getCourtCode(), session.getCourtRoom(), session.getDateOfHearing());
        });
    }

    private String saveCase(Case aCase) {
        telemetryService.trackCourtCaseEvent(aCase);
        courtCaseService.getCourtCase(aCase)
            .subscribe(this::postCaseEvent);
        return aCase.getCaseNo();
    }

    void postCaseEvent(CourtCase courtCase) {
        if (courtCase.isNew()) {
            eventBus.post(new CourtCaseMatchEvent(courtCase));
        }
        else {
            eventBus.post(new CourtCaseUpdateEvent(courtCase));
        }
    }

    private void logMessageReceipt(MessageHeader messageHeader) {
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
    }

    private Document trackCourtListReceipt(Document document) {
        Info info = document.getInfo();
        log.debug("Received court list for court {} on {}", info.getInfoSourceDetail().getOuCode(), info.getDateOfHearing().toString());
        telemetryService.trackCourtListEvent(document.getInfo());
        return document;
    }

}
