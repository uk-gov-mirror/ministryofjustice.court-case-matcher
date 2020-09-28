package uk.gov.justice.probation.courtcasematcher.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import static java.util.Optional.ofNullable;

@Service
@AllArgsConstructor
public class TelemetryService {

    static final String COURT_CODE_KEY = "courtCode";
    static final String COURT_ROOM_KEY = "courtRoom";
    static final String CASE_NO_KEY = "caseNo";
    static final String MATCHED_BY_KEY = "matchedBy";
    static final String MATCHES_KEY = "matches";
    static final String PNC_KEY = "pnc";
    static final String CRNS_KEY = "crns";
    static final String DATE_OF_HEARING_KEY = "dateOfHearing";

    private final TelemetryClient telemetryClient;

    public void trackEvent(TelemetryEventType eventType) {
        telemetryClient.trackEvent(eventType.eventName);
    }

    public void trackOffenderMatchEvent(String courtCode, String caseNo, SearchResponse searchResponse) {
        if (searchResponse == null) {
            return;
        }

        int matchCount = searchResponse.getMatchCount();
        Map<String, String> properties = new HashMap<>(6);
        ofNullable(courtCode)
            .ifPresent((code) -> properties.put(COURT_CODE_KEY, code));
        ofNullable(caseNo)
            .ifPresent((aCase) -> properties.put(CASE_NO_KEY, aCase));
        ofNullable(searchResponse.getMatchedBy())
            .filter((matchedBy) -> matchCount >= 1)
            .ifPresent((matchedBy) -> properties.put(MATCHED_BY_KEY, matchedBy.name()));
        ofNullable(searchResponse.getMatches())
            .ifPresent((matches -> {
                String allCrns = matches.stream()
                    .map(match -> match.getOffender().getOtherIds().getCrn())
                    .collect(Collectors.joining(","));
                properties.put(MATCHES_KEY, String.valueOf(matches.size()));
                properties.put(CRNS_KEY, allCrns);
            }));
        // TODO with strategic solution - when we're searching with PNC
        properties.put(PNC_KEY, null);

        TelemetryEventType eventType = TelemetryEventType.OFFENDER_PARTIAL_MATCH;
        if (searchResponse.isExactMatch()) {
            eventType = TelemetryEventType.OFFENDER_EXACT_MATCH;
        }
        else if (matchCount == 0){
            eventType = TelemetryEventType.OFFENDER_NO_MATCH;
        }
        telemetryClient.trackEvent(eventType.eventName, properties, Collections.emptyMap());

    }

    public void trackCourtCaseEvent(Case aCase) {

        Map<String, String> properties = new HashMap<>(4);
        ofNullable(aCase.getBlock().getSession().getCourtCode())
            .ifPresent((code) -> properties.put(COURT_CODE_KEY, code));
        ofNullable(aCase.getBlock().getSession().getCourtRoom())
            .ifPresent((courtRoom) -> properties.put(COURT_ROOM_KEY, courtRoom));
        ofNullable(aCase.getBlock().getSession().getDateOfHearing())
            .ifPresent((dateOfHearing) -> properties.put(DATE_OF_HEARING_KEY, dateOfHearing.toString()));
        ofNullable(aCase.getCaseNo())
            .ifPresent((caseNo) -> properties.put(CASE_NO_KEY, caseNo));

        telemetryClient.trackEvent(TelemetryEventType.COURT_CASE_RECEIVED.eventName, properties, Collections.emptyMap());
    }

    public void trackCourtListEvent(Info info) {

        Map<String, String> properties = new HashMap<>(2);
        ofNullable(info.getInfoSourceDetail().getOuCode())
            .ifPresent((courtCode) -> properties.put(COURT_CODE_KEY, courtCode));
        ofNullable(info.getDateOfHearing())
            .ifPresent((dateOfHearing) -> properties.put(DATE_OF_HEARING_KEY, dateOfHearing.toString()));

        telemetryClient.trackEvent(TelemetryEventType.COURT_LIST_RECEIVED.eventName, properties, Collections.emptyMap());
    }
}
