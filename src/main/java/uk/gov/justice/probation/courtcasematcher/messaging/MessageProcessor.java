package uk.gov.justice.probation.courtcasematcher.messaging;

import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseMatchEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseUpdateEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DocumentWrapper;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageProcessor {

    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus;

    private final TelemetryService telemetryService;

    private final CourtCaseService courtCaseService;

    @Autowired
    public MessageProcessor(EventBus eventBus,
                            TelemetryService telemetryService,
                            CourtCaseService courtCaseService) {
        super();
        this.eventBus = eventBus;
        this.telemetryService = telemetryService;
        this.courtCaseService = courtCaseService;
    }

    void process(ExternalDocumentRequest externalDocumentRequest, String messageId) {

        List<Document> documents = extractDocuments(externalDocumentRequest);

        documents.stream()
            .map(Document::getInfo)
            .distinct()
            .forEach(info -> trackCourtListReceipt(info, messageId));

        List<Session> sessions = documents
            .stream()
            .flatMap(document -> document.getData().getJob().getSessions().stream())
            .collect(Collectors.toList());

        saveCases(sessions, messageId);
    }

    private void saveCases(List<Session> sessions, String messageId) {
        sessions.forEach(session -> {
            log.debug("Starting to process cases in session court {}, room {}, date {}",
                session.getCourtCode(), session.getCourtRoom(), session.getDateOfHearing());

            List<String> cases = session.getBlocks().stream()
                .flatMap(block -> block.getCases().stream())
                .map(aCase -> saveCase(aCase, messageId))
                .collect(Collectors.toList());
            log.debug("Completed {} cases for {}, {}, {}", cases.size(), session.getCourtCode(), session.getCourtRoom(), session.getDateOfHearing());
        });
    }

    private String saveCase(Case aCase, String messageId) {
        telemetryService.trackCourtCaseEvent(aCase, messageId);
        courtCaseService.getCourtCase(aCase)
            .subscribe(this::postCaseEvent);
        return aCase.getCaseNo();
    }

    void postCaseEvent(CourtCase courtCase) {
        if (courtCase.shouldMatchToOffender()) {
            eventBus.post(new CourtCaseMatchEvent(courtCase));
        }
        else {
            eventBus.post(new CourtCaseUpdateEvent(courtCase));
        }
    }

    private void trackCourtListReceipt(Info info, String messageId) {
        log.debug("Received court list for court {} on {}", info.getOuCode(), info.getDateOfHearing().toString());
        telemetryService.trackCourtListEvent(info, messageId);
    }

    private List<Document> extractDocuments(ExternalDocumentRequest externalDocumentRequest) {
        return Optional.ofNullable(externalDocumentRequest.getDocumentWrapper())
            .map(DocumentWrapper::getDocument)
            .or(() -> Optional.of(Collections.emptyList()))
            .orElse(Collections.emptyList());
    }
}
