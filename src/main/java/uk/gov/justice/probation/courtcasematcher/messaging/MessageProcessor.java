package uk.gov.justice.probation.courtcasematcher.messaging;

import com.google.common.eventbus.EventBus;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageHeader;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

@Service
@Slf4j
public class MessageProcessor {

    @SuppressWarnings("UnstableApiUsage") // Not part of the final product
    private final EventBus eventBus;

    private final CourtCaseRestClient restClient;

    private final GatewayMessageParser parser;

    private final CaseMapper caseMapper;

    public MessageProcessor(GatewayMessageParser parser, CourtCaseRestClient restClient, EventBus eventBus, CaseMapper caseMapper) {
        super();
        this.parser = parser;
        this.restClient = restClient;
        this.eventBus = eventBus;
        this.caseMapper = caseMapper;
    }

    public void processAll(String message) {
        parse(message).ifPresent(this::processAll);
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

    private void processAll(List<Session> sessions) {
        List<Case> cases = sessions
            .stream()
            .flatMap(session -> session.getBlocks().stream())
            .flatMap(block -> block.getCases().stream())
            .collect(Collectors.toList());

        log.info("Received {} cases", cases.size());
        cases.forEach(this::process);
    }

    private void process(Case incomingCase) {
        CourtCase courtCase = match(incomingCase);
        store(courtCase);
    }

    private void store(CourtCase courtCase) {
        restClient.putCourtCase(courtCase.getCourtCode(), courtCase.getCaseNo(), courtCase);
    }

    private CourtCase match(Case incomingCase) {
        Optional<CourtCase> existingCase = restClient.getCourtCase(incomingCase.getBlock().getSession().getCourtCode(), incomingCase.getCaseNo()).blockOptional();
//        TODO: I think this class probably has too many responsibilities. I will implement here for now and review after to see if I can break it out into logical components
        return existingCase
                                            .map(courtCaseApi1 -> caseMapper.merge(incomingCase, courtCaseApi1))
//                TODO: At this point in the flow we need to introduce a new call to offender search to get the crn etc
                                            .orElse(caseMapper.newFromCase(incomingCase));
    }

    private void logMessageReceipt(MessageHeader messageHeader) {
        log.info("Received message UUID {}, from {}, original timestamp {}",
            messageHeader.getMessageID().getUuid(), messageHeader.getFrom(), messageHeader.getTimeStamp());
    }
}
