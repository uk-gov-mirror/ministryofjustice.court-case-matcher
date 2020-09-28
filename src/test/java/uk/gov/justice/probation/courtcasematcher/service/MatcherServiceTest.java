package uk.gov.justice.probation.courtcasematcher.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
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
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
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
    private static final String CRN = "X123456";
    private static final String PROBATION_STATUS = "Current";
    private static final String DEFAULT_PROBATION_STATUS = "No record";
    private static final String MATCHES_PROBATION_STATUS = "Possible nDelius record";

    private final LocalDate DEF_DOB = LocalDate.of(2000, 6, 17);
    private final String DEF_NAME = "Arthur MORGAN";

    private final OtherIds otherIds = OtherIds.builder()
        .crn(CRN)
        .cro("CRO")
        .pnc("PNC")
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

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private MatcherService matcherService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(MatcherService.class).getName());
        logger.addAppender(mockAppender);

        matcherService =
            new MatcherService(courtCaseRestClient, offenderSearchRestClient, DEFAULT_PROBATION_STATUS, MATCHES_PROBATION_STATUS);
    }

    @Test
    void givenIncomingDefendantDoesNotMatchAnOffender_whenMatchCalled_thenLog(){
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.empty());

        SearchResponse searchResponse = matcherService.getSearchResponse(DEF_NAME, DEF_DOB, COURT_CODE, CASE_NO).block();

        assertThat(searchResponse).isNull();
        LoggingEvent loggingEvent = captureFirstLogEvent();
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains("Match results for caseNo: 1600032952, courtCode: SHF - Empty response from OffenderSearchRestClient");
        verifyNoMoreInteractions(courtCaseRestClient);
    }

    @Test
    void givenMatchesToMultipleOffenders_whenMatchCalled_thenReturn(){
        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(multipleExactMatches));

        SearchResponse searchResponse = matcherService.getSearchResponse(DEF_NAME, DEF_DOB, COURT_CODE, CASE_NO).block();

        assertThat(searchResponse.getMatches()).hasSize(2);
        assertThat(searchResponse.getMatchedBy()).isSameAs(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(searchResponse.getProbationStatus()).isEqualTo(MATCHES_PROBATION_STATUS);
    }

    @Test
    void givenMatchesToSingleOffender_whenSearchResponse_thenReturnWithProbationStatus(){

        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(singleExactMatch));
        when(courtCaseRestClient.getProbationStatus(CRN)).thenReturn(Mono.just(PROBATION_STATUS));

        SearchResponse searchResponse = matcherService.getSearchResponse(DEF_NAME, DEF_DOB, COURT_CODE, CASE_NO).block();

        assertThat(searchResponse.getMatches()).hasSize(1);
        assertThat(searchResponse.getMatchedBy()).isSameAs(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(searchResponse.getProbationStatus()).isEqualTo(PROBATION_STATUS);
    }

    @Test
    void givenZeroMatches_whenSearchResponse_thenReturn(){

        when(offenderSearchRestClient.search(DEF_NAME, DEF_DOB)).thenReturn(Mono.just(noMatches));

        SearchResponse searchResponse = matcherService.getSearchResponse(DEF_NAME, DEF_DOB, COURT_CODE, CASE_NO).block();

        assertThat(searchResponse.getMatches()).hasSize(0);
        assertThat(searchResponse.getMatchedBy()).isSameAs(OffenderSearchMatchType.NOTHING);
        assertThat(searchResponse.getProbationStatus()).isEqualTo(DEFAULT_PROBATION_STATUS);
    }

    private LoggingEvent captureFirstLogEvent() {
        verify(mockAppender).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        return events.get(0);
    }
}
