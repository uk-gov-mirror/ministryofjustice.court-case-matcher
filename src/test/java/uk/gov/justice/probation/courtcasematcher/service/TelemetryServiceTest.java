package uk.gov.justice.probation.courtcasematcher.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.DataJob;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Document;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.InfoSourceDetail;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Job;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OtherIds;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CASE_NO_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.COURT_CODE_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.COURT_ROOM_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.CRNS_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.DATE_OF_HEARING_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.MATCHED_BY_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.MATCHES_KEY;
import static uk.gov.justice.probation.courtcasematcher.service.TelemetryService.PNC_KEY;

@DisplayName("Exercise TelemetryService")
@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    private static final String COURT_CODE = "B10JQ00";
    private static final String CASE_NO = "1234567890";
    private static final String CRN = "D12345";
    private static final String COURT_ROOM = "01";
    private static final LocalDate DATE_OF_HEARING = LocalDate.of(2020, Month.NOVEMBER, 5);
    private static Info info;
    private static Case aCase;

    @Captor
    private ArgumentCaptor<Map<String, String>> propertiesCaptor;

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private TelemetryService telemetryService;


    @BeforeAll
    static void beforeEach() {

        info = Info.builder()
            .infoSourceDetail(InfoSourceDetail.builder().ouCode(COURT_CODE).build())
            .dateOfHearing(DATE_OF_HEARING)
            .build();
        Document document = Document.builder().info(info).build();
        DataJob dataJob = DataJob.builder().document(document).build();
        Job job = Job.builder().dataJob(dataJob).build();

        Session session = Session.builder()
            .courtName("COURT_NAME")
            .courtRoom(COURT_ROOM)
            .dateOfHearing(DATE_OF_HEARING)
            .job(job)
            .build();

        aCase = Case.builder()
            .block(Block.builder()
                .session(session)
                .build())
            .caseNo(CASE_NO)
            .build();
    }

    @DisplayName("Simple record of event with no properties")
    @Test
    void whenMessageReceived_thenRecord() {
        telemetryService.trackEvent(TelemetryEventType.COURT_LIST_RECEIVED);

        verify(telemetryClient).trackEvent("PiCCourtListReceived");
    }

    @DisplayName("Record the event when an exact match happens")
    @Test
    void whenExactMatch_thenRecord() {
        Match match = buildMatch(CRN);
        SearchResponse response = SearchResponse.builder()
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .matches(List.of(match))
            .probationStatus("Current")
            .build();

        telemetryService.trackOffenderMatchEvent(COURT_CODE, CASE_NO, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderExactMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(6);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(MATCHES_KEY, "1"),
            entry(MATCHED_BY_KEY, OffenderSearchMatchType.ALL_SUPPLIED.name()),
            entry(CRNS_KEY, CRN),
            entry(PNC_KEY, null)
        );
    }

    @DisplayName("Record the event when a partial match happens with multiple offenders")
    @Test
    void whenPartialMatchEvent_thenRecord() {
        List<Match> matches = buildMatches(List.of(CRN, "X123454"));
        SearchResponse response = SearchResponse.builder()
            .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
            .matches(matches)
            .probationStatus("Possible nDelius records")
            .build();

        telemetryService.trackOffenderMatchEvent(COURT_CODE, CASE_NO, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(6);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(MATCHES_KEY, "2"),
            entry(MATCHED_BY_KEY, OffenderSearchMatchType.PARTIAL_NAME.name()),
            entry(CRNS_KEY, CRN + "," + "X123454"),
            entry(PNC_KEY, null)
        );
    }

    @DisplayName("Record the event when a partial match happens with a single offender")
    @Test
    void whenPartialToSingleOffenderMatchEvent_thenRecord() {
        SearchResponse response = SearchResponse.builder()
            .matchedBy(OffenderSearchMatchType.PARTIAL_NAME)
            .matches(List.of(buildMatch(CRN)))
            .probationStatus("Possible nDelius records")
            .build();

        telemetryService.trackOffenderMatchEvent(COURT_CODE, CASE_NO, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderPartialMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(6);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(MATCHES_KEY, "1"),
            entry(MATCHED_BY_KEY, OffenderSearchMatchType.PARTIAL_NAME.name()),
            entry(CRNS_KEY, CRN),
            entry(PNC_KEY, null)
        );
    }

    @DisplayName("Record the event when there is no match")
    @Test
    void whenNoMatchEvent_thenRecord() {
        SearchResponse response = SearchResponse.builder()
            .matchedBy(OffenderSearchMatchType.NOTHING)
            .probationStatus("No record")
            .build();

        telemetryService.trackOffenderMatchEvent(COURT_CODE, CASE_NO, response);

        verify(telemetryClient).trackEvent(eq("PiCOffenderNoMatch"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();
        assertThat(properties).hasSize(3);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(CASE_NO_KEY, CASE_NO),
            entry(PNC_KEY, null)
        );
    }

    @DisplayName("Record the event when a court case is received")
    @Test
    void whenCourtCaseReceived_thenRecord() {

        telemetryService.trackCourtCaseEvent(aCase);

        verify(telemetryClient).trackEvent(eq("PiCCourtCaseReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();

        assertThat(properties).hasSize(4);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(COURT_ROOM_KEY, COURT_ROOM),
            entry(CASE_NO_KEY, CASE_NO),
            entry(DATE_OF_HEARING_KEY, "2020-11-05")
        );
    }

    @Test
    void whenCourtListReceived_thenRecord() {

        telemetryService.trackCourtListEvent(info);

        verify(telemetryClient).trackEvent(eq("PiCCourtListReceived"), propertiesCaptor.capture(), eq(Collections.emptyMap()));

        Map<String, String> properties = propertiesCaptor.getValue();

        assertThat(properties).hasSize(2);
        assertThat(properties).contains(
            entry(COURT_CODE_KEY, COURT_CODE),
            entry(DATE_OF_HEARING_KEY, "2020-11-05")
        );
    }

    private Match buildMatch(String crn) {
        return Match.builder()
                .offender(Offender.builder()
                    .otherIds(OtherIds.builder().crn(crn).build())
                    .build())
                .build();
    }

    private List<Match> buildMatches(List<String> crns) {
        return crns.stream()
            .map(this::buildMatch)
            .collect(Collectors.toList());
    }
}
