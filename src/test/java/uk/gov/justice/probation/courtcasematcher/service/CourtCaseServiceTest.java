package uk.gov.justice.probation.courtcasematcher.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtCaseServiceTest {

    private static final LocalDate JAN_1 = LocalDate.of(2020, Month.JANUARY, 1);
    private static final LocalDate JAN_3 = LocalDate.of(2020, Month.JANUARY, 3);
    private static final String COURT_CODE = "B10JQ00";
    private static final String COURT_ROOM = "1";
    private static final String CASE_NO = "1234567890";
    private static final String CRN = "X340741";
    private static final Long CASE_ID = 321344L;

    @Mock
    private CourtCaseRestClient restClient;

    @InjectMocks
    private CourtCaseService courtCaseService;

    @DisplayName("Purge with 5 cases across 4 sessions and 2 days")
    @Test
    void purgeAbsent() {

        Session sessionJan1a = Session.builder().dateOfHearing(JAN_1).courtCode(COURT_CODE).build();
        Session sessionJan1b = Session.builder().dateOfHearing(JAN_1).courtCode(COURT_CODE).build();

        Session sessionJan3a = Session.builder().dateOfHearing(JAN_3).courtCode(COURT_CODE).build();
        Session sessionJan3b = Session.builder().dateOfHearing(JAN_3).courtCode(COURT_CODE).build();

        Case aCase10 = Case.builder().caseNo("1010").block(Block.builder().session(sessionJan1a).build()).build();
        Case aCase20 = Case.builder().caseNo("1011").block(Block.builder().session(sessionJan1b).build()).build();
        Case aCase21 = Case.builder().caseNo("1030").block(Block.builder().session(sessionJan3a).build()).build();
        Case aCase30 = Case.builder().caseNo("1031").block(Block.builder().session(sessionJan3a).build()).build();
        Case aCase31 = Case.builder().caseNo("1032").block(Block.builder().session(sessionJan3b).build()).build();

        List<Case> allCases = asList(aCase20, aCase21, aCase10, aCase30, aCase31);

        courtCaseService.purgeAbsent(COURT_CODE, Set.of(JAN_1, JAN_3), allCases);

        Map<LocalDate, List<String>> expected = Map.of(JAN_1, asList("1010", "1011"), JAN_3, asList("1030", "1031", "1032"));
        verify(restClient).purgeAbsent(COURT_CODE, expected);
    }

    @DisplayName("Purge with 2 cases across 1 sessions and 2 days. One day has no cases.")
    @Test
    void purgeAbsentNoCases() {


        Session sessionJan3 = Session.builder().dateOfHearing(JAN_3).courtCode(COURT_CODE).build();

        Case aCase30 = Case.builder().caseNo("1031").block(Block.builder().session(sessionJan3).build()).build();
        Case aCase31 = Case.builder().caseNo("1032").block(Block.builder().session(sessionJan3).build()).build();

        courtCaseService.purgeAbsent(COURT_CODE, Set.of(JAN_1, JAN_3), asList(aCase30, aCase31));

        Map<LocalDate, List<String>> expected = Map.of(JAN_1, Collections.emptyList(), JAN_3, asList("1031", "1032"));
        verify(restClient).purgeAbsent(COURT_CODE, expected);
    }

    @DisplayName("Save court case.")
    @Test
    void saveCourtCase() {

        CourtCase courtCase = CourtCase.builder().caseNo(CASE_NO).courtCode(COURT_CODE).build();

        courtCaseService.saveCourtCase(courtCase);

        verify(restClient).putCourtCase(COURT_CODE, CASE_NO, courtCase);
    }

    @DisplayName("Incoming gateway case which is merged with the existing.")
    @Test
    void givenExistingCase_whenGetCourtCase_thenMergeAndReturn() {
        Case aCase = buildCase();

        CourtCase courtCase = CourtCase.builder().caseId(Long.toString(CASE_ID)).caseNo(CASE_NO).courtCode(COURT_CODE).courtRoom("2").build();
        when(restClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.just(courtCase));

        CourtCase updatedCourtCase = courtCaseService.getCourtCase(aCase).block();

        assertThat(updatedCourtCase.getCourtRoom()).isEqualTo(COURT_ROOM);
        assertThat(courtCase.getCourtRoom()).isNotEqualTo(aCase.getBlock().getSession().getCourtRoom());
        verify(restClient).getCourtCase(COURT_CODE, CASE_NO);
    }

    @DisplayName("Get court case which is new, return a transformed copy.")
    @Test
    void givenNewCase_whenGetCourtCase_thenReturn() {
        Case aCase = buildCase();

        when(restClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());

        CourtCase newCourtCase = courtCaseService.getCourtCase(aCase).block();

        assertThat(newCourtCase.getCourtCode()).isSameAs(COURT_CODE);
        assertThat(newCourtCase.getCaseNo()).isSameAs(CASE_NO);
        verify(restClient).getCourtCase(COURT_CODE, CASE_NO);
    }

    @DisplayName("Save a court case with a search response.")
    @Test
    void givenSearchResponse_whenCreateCourtCase_thenPutCase() {

        SearchResponse searchResponse = SearchResponse.builder().build();
        CourtCase courtCase = CourtCase.builder()
                            .caseId(Long.toString(CASE_ID))
                            .caseNo(CASE_NO)
                            .courtCode(COURT_CODE)
                            .groupedOffenderMatches(GroupedOffenderMatches.builder().matches(Collections.emptyList()).build())
                            .build();

        courtCaseService.createCase(courtCase, SearchResult.builder().searchResponse(searchResponse).build());

        verify(restClient).putCourtCase(COURT_CODE, CASE_NO, courtCase);
    }

    @DisplayName("Save a court case without a search response.")
    @Test
    void givenNoSearchResponse_whenCreateCourtCase_thenReturn() {

        CourtCase courtCase = CourtCase.builder().caseId(Long.toString(CASE_ID)).caseNo(CASE_NO).courtCode(COURT_CODE).build();

        courtCaseService.createCase(courtCase, null);

        verify(restClient).putCourtCase(COURT_CODE, CASE_NO, courtCase);
    }

    @DisplayName("Fetch and update probation status")
    @Test
    void whenUpdateProbationStatus_thenMergeAndReturn() {

        LocalDate localDate = LocalDate.of(2020, Month.AUGUST, 20);
        CourtCase courtCase = CourtCase.builder().crn(CRN).courtCode(COURT_CODE).caseNo(CASE_NO).probationStatus("Previously known")
            .probationStatusActual("PREVIOUSLY_KNOWN").build();
        ProbationStatusDetail probationStatusDetail = ProbationStatusDetail.builder()
                                                                    .status("CURRENT")
                                                                    .preSentenceActivity(true)
                                                                    .previouslyKnownTerminationDate(localDate)
                                                                    .build();
        when(restClient.getProbationStatusDetail(CRN)).thenReturn(Mono.just(probationStatusDetail));

        CourtCase courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(courtCaseResult.getProbationStatus()).isEqualTo("CURRENT");
        assertThat(courtCaseResult.getPreviouslyKnownTerminationDate()).isEqualTo(localDate);
        assertThat(courtCaseResult.getBreach()).isNull();
        assertThat(courtCaseResult.isPreSentenceActivity()).isTrue();
        verify(restClient).getProbationStatusDetail(CRN);
    }

    @DisplayName("When rest client fails to fetch updated probation status then return the original")
    @Test
    void givenFailedCallToRestClient_whenUpdateProbationStatus_thenReturnInput() {

        CourtCase courtCase = CourtCase.builder().crn(CRN).build();
        when(restClient.getProbationStatusDetail(CRN)).thenReturn(Mono.empty());

        CourtCase courtCaseResult = courtCaseService.updateProbationStatusDetail(courtCase).block();

        assertThat(courtCaseResult).isSameAs(courtCase);
        verify(restClient).getProbationStatusDetail(CRN);
    }

    private Case buildCase() {
        Session session = Session.builder()
            .courtCode(COURT_CODE)
            .courtRoom(COURT_ROOM)
            .dateOfHearing(LocalDate.of(2020, Month.AUGUST, 26))
            .start(LocalTime.of(9,0))
            .build();

        Block block = Block.builder()
            .session(session)
            .build();

        return Case.builder()
            .block(block)
            .caseNo(CASE_NO)
            .id(CASE_ID)
            .build();
    }
}
