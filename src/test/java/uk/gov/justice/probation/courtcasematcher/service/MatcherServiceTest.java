package uk.gov.justice.probation.courtcasematcher.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
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
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Block;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Session;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class MatcherServiceTest {
    private static final String COURT_CODE = "SHF";
    private static final String CASE_NO = "1600032952";
    private static final long REST_CLIENT_WAIT_MS = 2000;

    private final LocalDate DEF_DOB = LocalDate.of(2000, 6, 17);
    private final String DEF_NAME = "Arthur MORGAN";
    private final Case incomingCase = Case.builder()
            .caseNo(CASE_NO)
            .def_dob(DEF_DOB)
            .def_name(DEF_NAME)
            .block(Block.builder()
                    .session(Session.builder()
                            .courtCode(COURT_CODE)
                            .build())
                    .build())
            .build();
    private final CourtCase courtCase = CourtCase.builder()
            .caseNo(CASE_NO)
            .courtCode(COURT_CODE)
            .build();
    private final Offender offender = Offender.builder()
            .build();
    private final SearchResponse singleExactMatch = SearchResponse.builder()
            .matches(Collections.singletonList(Match.builder()
                    .offender(offender)
                    .build()))
            .matchedBy(MatchType.ALL_SUPPLIED)
            .build();
    private final SearchResponse multipleExactMatches = SearchResponse.builder()
            .matches(Arrays.asList(
                    Match.builder()
                    .offender(offender)
                    .build(),
                    Match.builder()
                    .offender(offender)
                    .build()))
            .matchedBy(MatchType.ALL_SUPPLIED)
            .build();
    private final SearchResponse noMatches = SearchResponse.builder()
            .matchedBy(MatchType.NOTHING)
            .matches(Collections.emptyList())
            .build();
    private final SearchResponse singleFuzzyMatch = SearchResponse.builder()
            .matches(Collections.singletonList(Match.builder()
                    .offender(offender)
                    .build()))
            .matchedBy(MatchType.PARTIAL_NAME_DOB_LENIENT)
            .build();

    @Mock
    private CourtCaseRestClient courtCaseRestClient;
    @Mock
    private CaseMapper caseMapper;
    @Mock
    private OffenderSearchRestClient offenderSearchRestClient;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    private Logger logger;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private MatcherService matcherService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        logger = (Logger) getLogger(LoggerFactory.getLogger(MatcherService.class).getName());
        logger.addAppender(mockAppender);

        matcherService = new MatcherService(courtCaseRestClient, offenderSearchRestClient, caseMapper);
    }

    @Test
    public void givenIncomingCaseMatchesExisting_whenMatchCalled_thenMergeAndStore() {
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.just(courtCase));
        when(caseMapper.merge(incomingCase, courtCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).merge(incomingCase, courtCase);
        verify(caseMapper, never()).newFromCaseAndOffender(incomingCase, offender);
        verify(caseMapper, never()).newFromCase(incomingCase);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItDoesNotMatchAnOffender_whenMatchCalled_thenCreateANewRecord(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.empty());
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(incomingCase);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCaseAndOffender(incomingCase, offender);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItExactlyMatchesASingleOffender_whenMatchCalled_thenCreateANewRecordWithOffenderData(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(singleExactMatch));
        when(caseMapper.newFromCaseAndOffender(incomingCase, offender)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCaseAndOffender(incomingCase, offender);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCase(incomingCase);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItExactlyMatchesAMultipleOffenders_whenMatchCalled_thenCreateANewRecordWithoutOffenderData(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(multipleExactMatches));
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(incomingCase);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCaseAndOffender(incomingCase, offender);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }

    @Test
    void givenIncomingCaseDoesNotMatchExistingCase_andItHasNoExactMatches_whenMatchCalled_thenCreateANewRecordWithoutOffenderData(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(noMatches));
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        verify(caseMapper).newFromCase(incomingCase);
        verify(caseMapper, never()).merge(any(Case.class), eq(courtCase));
        verify(caseMapper, never()).newFromCaseAndOffender(incomingCase, offender);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
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
        verify(caseMapper, never()).newFromCaseAndOffender(incomingCase, offender);
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }

    @Test
    public void whenMatchesReturned_thenLogTheDetails() {
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(singleExactMatch));
        when(caseMapper.newFromCaseAndOffender(incomingCase, offender)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        LoggingEvent loggingEvent = captureLogEvent();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Match results for caseNo: 1600032952, courtCode: SHF - matchedBy: ALL_SUPPLIED, matchCount: 1");
    }

    @Test
    void whenEmptyMonoReturned_thenLogDetails(){
        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.empty());
        when(caseMapper.newFromCase(incomingCase)).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(mock(Disposable.class));

        matcherService.match(incomingCase);

        LoggingEvent loggingEvent = captureLogEvent();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage().trim())
                .contains("Match results for caseNo: 1600032952, courtCode: SHF - Empty response from OffenderSearchRestClient");
    }

    private LoggingEvent captureLogEvent() {
        verify(mockAppender).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        return events.get(0);
    }
}