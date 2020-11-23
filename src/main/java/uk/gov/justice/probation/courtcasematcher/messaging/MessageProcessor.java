package uk.gov.justice.probation.courtcasematcher.messaging;

import java.util.List;
import java.util.stream.Collectors;
import com.google.common.eventbus.EventBus;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseMatchEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseUpdateEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.DefendantType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@Setter
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

    void process(ExternalDocumentRequest externalDocumentRequest) {
        List<Session> sessions = externalDocumentRequest
            .getDocumentWrapper()
            .getDocument()
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
        if (courtCase.isNew() && DefendantType.PERSON == courtCase.getDefendantType()) {
            eventBus.post(new CourtCaseMatchEvent(courtCase));
        }
        else {
            eventBus.post(new CourtCaseUpdateEvent(courtCase));
        }
    }

    private Document trackCourtListReceipt(Document document) {
        Info info = document.getInfo();
        log.debug("Received court list for court {} on {}", info.getInfoSourceDetail().getOuCode(), info.getDateOfHearing().toString());
        telemetryService.trackCourtListEvent(document.getInfo());
        return document;
    }

}
