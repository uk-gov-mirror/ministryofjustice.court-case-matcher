package uk.gov.justice.probation.courtcasematcher.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OtherIds;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

@ExtendWith(MockitoExtension.class)
class MatcherServiceTest {
    private static final String COURT_CODE = "SHF";
    private static final String CASE_NO = "1600032952";
    private static final long REST_CLIENT_WAIT_MS = 2000;
    private static final String CRN = "X123456";

    private final LocalDate DEF_DOB = LocalDate.of(2000, 6, 17);
    private final String DEF_NAME = "Arthur MORGAN";
    private final Case incomingCase = Case.builder()
            .caseNo(CASE_NO)
            .def_dob(DEF_DOB)
            .def_name(DEF_NAME)
            .block(Block.builder()
                    .session(Session.builder()
                            .courtName(COURT_CODE)
                            .build())
                    .build())
            .build();
    private final OtherIds otherIds = OtherIds.builder()
        .crn(CRN)
        .cro("CRO")
        .pnc("PNC")
        .build();
    private final CourtCase courtCase = CourtCase.builder()
            .caseNo(CASE_NO)
            .courtCode(COURT_CODE)
            .build();
    private final Offender offender = Offender.builder()
            .otherIds(otherIds)
            .build();
    private final SearchResponse singleExactMatch = SearchResponse.builder()
            .matches(singletonList(Match.builder()
                    .offender(offender)
                    .build()))
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .build();
    private final SearchResponse multipleExactMatches = SearchResponse.builder()
            .matches(Arrays.asList(
                    Match.builder()
                    .offender(offender)
                    .build(),
                    Match.builder()
                    .offender(offender)
                    .build()))
            .matchedBy(OffenderSearchMatchType.ALL_SUPPLIED)
            .build();
    private final SearchResponse noMatches = SearchResponse.builder()
            .matchedBy(OffenderSearchMatchType.NOTHING)
            .matches(Collections.emptyList())
            .build();
    private final SearchResponse singleFuzzyMatch = SearchResponse.builder()
            .matches(singletonList(Match.builder()
                    .offender(offender)
                    .build()))
            .matchedBy(OffenderSearchMatchType.PARTIAL_NAME_DOB_LENIENT)
            .build();

    private final OffenderMatch offenderMatch = OffenderMatch.builder()
        .matchType(MatchType.NAME_DOB)
        .confirmed(false)
        .matchIdentifiers(MatchIdentifiers.builder()
            .crn(CRN)
            .cro("CRO")
            .pnc("PNC")
            .build())
        .build();

    private final GroupedOffenderMatches groupedOffenderMatches = GroupedOffenderMatches.builder()
        .matches(singletonList(offenderMatch))
        .build();

    @Mock
    private CourtCaseRestClient courtCaseRestClient;
    @Mock
    private CaseMapper caseMapper;
    @Mock
    private OffenderSearchRestClient offenderSearchRestClient;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private MatcherService matcherService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(MatcherService.class).getName());
        logger.addAppender(mockAppender);

        matcherService = new MatcherService(courtCaseRestClient, offenderSearchRestClient, caseMapper);
    }

    @Test
    public void givenIncomingCaseMatchesExisting_whenMatchCalled_thenMergeAndStore() {
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.just(courtCase));
        when(caseMapper.merge(incomingCase, courtCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(COURT_CODE, CASE_NO, courtCase)).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).merge(incomingCase, courtCase);
        verify(caseMapper, never()).newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class));
        verify(caseMapper, never()).newFromCase(incomingCase);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(COURT_CODE, CASE_NO, courtCase);
        verify(courtCaseRestClient, never()).postMatches(COURT_CODE, CASE_NO, groupedOffenderMatches);
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItDoesNotMatchAnOffender_whenMatchCalled_thenCreateANewRecord(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.empty());
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(COURT_CODE, CASE_NO, courtCase)).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(incomingCase);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class));
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(COURT_CODE, CASE_NO, courtCase);
        verify(courtCaseRestClient, never()).postMatches(COURT_CODE, CASE_NO, groupedOffenderMatches);
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItExactlyMatchesASingleOffender_whenMatchCalled_thenCreateANewRecordWithOffenderData(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(singleExactMatch));
        when(caseMapper.newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class))).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(COURT_CODE, CASE_NO, courtCase)).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class));
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCase(incomingCase);
        verify(courtCaseRestClient).getCourtCase(COURT_CODE, CASE_NO);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(COURT_CODE, CASE_NO, courtCase);
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItExactlyMatchesAMultipleOffenders_whenMatchCalled_thenCreateANewRecordWithoutOffenderData(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(multipleExactMatches));
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(COURT_CODE, CASE_NO, courtCase)).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(incomingCase);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class));
        verify(courtCaseRestClient).getCourtCase(COURT_CODE, CASE_NO);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(COURT_CODE, CASE_NO, courtCase);
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItHasNoExactMatches_whenMatchCalled_thenCreateANewRecordWithoutOffenderData(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(noMatches));
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(COURT_CODE, CASE_NO, courtCase)).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(incomingCase);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class));
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(COURT_CODE, CASE_NO, courtCase);
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItHasOnePartialMatch_whenMatchCalled_thenCreateANewRecordWithoutOffenderData(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(singleFuzzyMatch));
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(incomingCase);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class));
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(COURT_CODE, CASE_NO, courtCase);
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    public void whenMatchesReturned_thenPostMatchesAndLogTheDetails() {
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(singleExactMatch));
        when(caseMapper.newFromCaseAndOffender(eq(incomingCase), eq(offender), any(GroupedOffenderMatches.class))).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(COURT_CODE, CASE_NO, courtCase)).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        LoggingEvent loggingEvent = captureFirstLogEvent();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Match results for caseNo: 1600032952, courtCode: SHF - matchedBy: ALL_SUPPLIED, matchCount: 1");

        verify(courtCaseRestClient).putCourtCase(COURT_CODE, CASE_NO, courtCase);
        verify(courtCaseRestClient).getCourtCase(COURT_CODE, CASE_NO);
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    void whenEmptyMonoReturned_thenLogDetails(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.empty());
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(COURT_CODE, CASE_NO, courtCase)).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        LoggingEvent loggingEvent = captureFirstLogEvent();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Match results for caseNo: 1600032952, courtCode: SHF - Empty response from OffenderSearchRestClient");
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    private LoggingEvent captureFirstLogEvent() {
        verify(mockAppender).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        return events.get(0);
    }
}
