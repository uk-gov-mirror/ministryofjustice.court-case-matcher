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
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OtherIds;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.restclient.OffenderSearchRestClient;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class MatcherServiceTest {
    private static final String COURT_CODE = "SHF";
    private static final String CASE_NO = "1600032952";
    private static final String CRN = "X123456";
    private static final String PROBATION_STATUS = "Current";
    private static final String DEFAULT_PROBATION_STATUS = "No record";
    private static final String MATCHES_PROBATION_STATUS = "Possible nDelius record";
    private static final String PNC = "PNC";

    private static final LocalDate DEF_DOB = LocalDate.of(2000, 6, 17);
    private static final Name DEF_NAME = Name.builder().forename1("Arthur")
                                                .surname("MORGAN")
                                                .build();

    private static final CourtCase COURT_CASE = CourtCase.builder()
        .caseNo(CASE_NO)
        .courtCode(COURT_CODE)
        .name(DEF_NAME)
        .defendantDob(DEF_DOB)
        .defendantName(DEF_NAME.getFullName())
        .pnc(PNC)
        .build();


    private final OtherIds otherIds = OtherIds.builder()
        .crn(CRN)
        .cro("CRO")
        .pnc(PNC)
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

    @Mock
    private CourtCaseRestClient courtCaseRestClient;

    @Mock
    private OffenderSearchRestClient offenderSearchRestClient;

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Mock
    private MatchRequest.Factory matchRequestFactory;

    @Mock
    private MatchRequest matchRequest;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private MatcherService matcherService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(MatcherService.class).getName());
        logger.addAppender(mockAppender);

        matcherService =
            new MatcherService(courtCaseRestClient, offenderSearchRestClient, matchRequestFactory, DEFAULT_PROBATION_STATUS, MATCHES_PROBATION_STATUS);

    }

    @Test
    void givenIncomingDefendantDoesNotMatchAnOffender_whenMatchCalled_thenLog(){
        when(matchRequestFactory.buildFrom(COURT_CASE)).thenReturn(matchRequest);
        when(offenderSearchRestClient.search(matchRequest)).thenReturn(Mono.empty());

        var searchResult = matcherService.getSearchResponse(COURT_CASE).block();

        assertThat(searchResult).isNull();
        LoggingEvent loggingEvent = captureFirstLogEvent();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains("Match results for caseNo: 1600032952, courtCode: SHF - Empty response from OffenderSearchRestClient");
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    void givenException_whenBuildingMatchRequest_thenLog(){
        when(matchRequestFactory.buildFrom(COURT_CASE)).thenThrow(new IllegalArgumentException("This is the reason"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> matcherService.getSearchResponse(COURT_CASE).block());

        LoggingEvent loggingEvent = captureFirstLogEvent();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains("Unable to create MatchRequest for caseNo: 1600032952, courtCode: SHF");
        assertThat(loggingEvent.getThrowableProxy().getClassName()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("This is the reason");
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    void givenMatchesToMultipleOffenders_whenMatchCalled_thenReturn(){
        when(matchRequestFactory.buildFrom(COURT_CASE)).thenReturn(matchRequest);
        when(offenderSearchRestClient.search(matchRequest)).thenReturn(Mono.just(multipleExactMatches));

        var searchResult = matcherService.getSearchResponse(COURT_CASE).block();
        var searchResponse = searchResult.getSearchResponse();

        assertThat(searchResult.getMatchRequest()).isEqualTo(matchRequest);
        assertThat(searchResponse.getMatches()).hasSize(2);
        assertThat(searchResponse.getMatchedBy()).isSameAs(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(searchResponse.getProbationStatusDetail().getProbationStatus()).isEqualTo(MATCHES_PROBATION_STATUS);
    }

    @Test
    void givenMatchesToSingleOffender_whenSearchResponse_thenReturnWithProbationStatus(){
        ProbationStatusDetail detail = ProbationStatusDetail.builder().probationStatus(PROBATION_STATUS).build();

        when(matchRequestFactory.buildFrom(COURT_CASE)).thenReturn(matchRequest);
        when(offenderSearchRestClient.search(matchRequest)).thenReturn(Mono.just(singleExactMatch));
        when(courtCaseRestClient.getProbationStatusDetail(CRN)).thenReturn(Mono.just(detail));

        var searchResult = matcherService.getSearchResponse(COURT_CASE).block();
        var searchResponse = searchResult.getSearchResponse();

        assertThat(searchResult.getMatchRequest()).isEqualTo(matchRequest);
        assertThat(searchResponse.getMatchedBy()).isSameAs(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(searchResponse.getProbationStatusDetail()).isSameAs(detail);
    }

    @Test
    void givenZeroMatches_whenSearchResponse_thenReturn(){
        when(matchRequestFactory.buildFrom(COURT_CASE)).thenReturn(matchRequest);
        when(offenderSearchRestClient.search(matchRequest)).thenReturn(Mono.just(noMatches));

        var searchResult = matcherService.getSearchResponse(COURT_CASE).block();
        var searchResponse = searchResult.getSearchResponse();

        assertThat(searchResult.getMatchRequest()).isEqualTo(matchRequest);
        assertThat(searchResponse.getMatches()).hasSize(0);
        assertThat(searchResponse.getMatchedBy()).isSameAs(OffenderSearchMatchType.NOTHING);
        assertThat(searchResponse.getProbationStatusDetail().getProbationStatus()).isEqualTo(DEFAULT_PROBATION_STATUS);
    }

    private LoggingEvent captureFirstLogEvent() {
        verify(mockAppender).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        return events.get(0);
    }
}
