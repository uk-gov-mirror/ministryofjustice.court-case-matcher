package uk.gov.justice.probation.courtcasematcher.restclient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.eventbus.EventBus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseSuccessEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Address;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.GroupedOffenderMatches;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.MatchIdentifiers;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.Offence;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.OffenderMatch;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class CourtCaseRestClientIntTest {

    private static final String COURT_CODE = "SHF";
    private static final String CASE_NO = "12345";
    private static final String NEW_CASE_NO = "999999";
    private static final int WEB_CLIENT_TIMEOUT_MS = 5000;

    private static final GroupedOffenderMatches matches = GroupedOffenderMatches.builder()
        .matches(Collections.singletonList(OffenderMatch.builder()
            .matchType(MatchType.NAME_DOB)
            .matchIdentifiers(MatchIdentifiers.builder()
                .crn("X123456")
                .cro("E1324/11")
                .pnc("PNC")
                .build())
            .build()))
        .build();

    private static final CourtCase A_CASE = CourtCase.builder()
        .caseId("1246257")
        .caseNo(CASE_NO)
        .courtCode(COURT_CODE)
        .groupedOffenderMatches(matches)
        .build();

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @MockBean
    private EventBus eventBus;

    @Autowired
    private CourtCaseRestClient restClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8090)
            .usingFilesUnderClasspath("mocks"));

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        Logger logger = (Logger) getLogger(LoggerFactory.getLogger(CourtCaseRestClient.class).getName());
        logger.addAppender(mockAppender);
    }


    @Test
    public void whenGetCourtCase_thenMakeRestCallToCourtCaseService() {

        LocalDateTime startTime = LocalDateTime.of(2020, Month.JANUARY, 13, 9, 0, 0);
        Address address = Address.builder()
            .line1("27")
            .line2("Elm Place")
            .line3("Bangor")
            .postcode("ad21 5dr")
            .build();

        Offence offenceApi = Offence.builder()
            .offenceSummary("On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.")
            .offenceTitle("Theft from a shop")
            .act("Contrary to section 1(1) and 7 of the Theft Act 1968.")
            .build();

        CourtCase expected = CourtCase.builder()
            .caseId("1246257")
            .crn("X320741")
            .pnc("D/1234560BC")
            .listNo("2nd")
            .courtCode("SHF")
            .courtRoom("1")
            .sessionStartTime(startTime)
            .probationStatus("Current")
            .breach(Boolean.TRUE)
            .suspendedSentenceOrder(Boolean.FALSE)
            .caseNo("12345")
            .defendantAddress(address)
            .defendantDob(LocalDate.of(1977, Month.DECEMBER, 11))
            .defendantName("Mr Dylan Adam Armstrong")
            .defendantSex("M")
            .nationality1("British")
            .nationality2("Czech")
            .offences(Collections.singletonList(offenceApi))
            .build();

        Optional<CourtCase> optional = restClient.getCourtCase(COURT_CODE, "123456").blockOptional();

        assertThat(optional.get()).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void givenUnknownCaseNo_whenGetCourtCase_thenReturnEmptyOptional() {

        Optional<CourtCase> optional = restClient.getCourtCase(COURT_CODE, NEW_CASE_NO).blockOptional();

        assertThat(optional.isPresent()).isFalse();
    }

    @Test
    public void whenPutCourtCase_thenMakeRestCallToCourtCaseService() {

        restClient.putCourtCase(COURT_CODE, CASE_NO, A_CASE);

        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(any(CourtCaseSuccessEvent.class));
    }

    @Test
    public void givenUnknownCourt_whenPutCourtCase_thenFailureEvent() {

        String unknownCourtCode = "XXX";
        CourtCase courtCaseApi = CourtCase.builder()
            .caseNo("12345")
            .courtCode(unknownCourtCode)
            .build();

        restClient.putCourtCase(unknownCourtCode, CASE_NO, courtCaseApi);

        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(any(CourtCaseFailureEvent.class));
    }

    @Test
    public void whenRestClientThrows500OnPut_ThenFailureEvent() {
        restClient.putCourtCase("X500", CASE_NO, A_CASE);
        verify(eventBus, timeout(WEB_CLIENT_TIMEOUT_MS)).post(any(CourtCaseFailureEvent.class));
    }

    @Test
    public void whenPostOffenderMatches_thenMakeRestCallToCourtCaseService() {

        restClient.postMatches(COURT_CODE, CASE_NO, matches);

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    public void givenUnknownCourt_whenPostOffenderMatches_thenReportErrorToEventBus() {

        GroupedOffenderMatches matches = GroupedOffenderMatches.builder()
            .matches(Collections.singletonList(OffenderMatch.builder()
                .matchType(MatchType.NAME_DOB)
                .matchIdentifiers(MatchIdentifiers.builder()
                    .crn("X99999")
                    .cro("E1324/11")
                    .pnc("PNC")
                    .build())
                .build()))
            .build();

        restClient.postMatches("XXX", CASE_NO, matches);

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    public void whenRestClientThrows500OnPostMatches_ThenFailureEvent() {

        restClient.postMatches("X500", CASE_NO, matches);

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    public void whenRestClientCalledWithEmptyOptional_ThenFailureEvent() {

        restClient.postMatches(COURT_CODE, CASE_NO, null);

        verify(mockAppender, never()).doAppend(captorLoggingEvent.capture());
    }

    @Test
    public void whenPurgeAbsent_ThenPutAndLog() {

        Map<LocalDate, List<String>> casesByDate = Map.of(LocalDate.of(2020, Month.JANUARY, 1), Collections.emptyList());
        restClient.purgeAbsent(COURT_CODE, casesByDate);

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage()).isEqualTo("Successful PUT of all cases for purge in court case service for court SHF");
    }

    @Test
    public void givenUnknownCourt_whenPurgeAbsent_ThenReturn404() {

        Map<LocalDate, List<String>> casesByDate = Map.of(LocalDate.of(2020, Month.JANUARY, 1), Collections.emptyList());
        restClient.purgeAbsent("XXX", casesByDate);

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage()).contains("Unexpected exception when applying PUT to purge absent cases for court 'XXX'");
    }

    @Test
    public void whenRestClientThrows500OnPurgeAbsent_ThenFailureEvent() {

        Map<LocalDate, List<String>> casesByDate = Map.of(LocalDate.of(2020, Month.JANUARY, 1), Collections.emptyList());
        restClient.purgeAbsent("X500", casesByDate);

        verify(mockAppender, timeout(WEB_CLIENT_TIMEOUT_MS)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage()).contains("Unexpected exception when applying PUT to purge absent cases for court 'X500'");
    }

    @Test
    public void whenGetOffenderProbationStatus_thenMakeRestCallToCourtCaseService() {

        Optional<String> optional = restClient.getOffenderProbationStatus("X320741").blockOptional();

        assertThat(optional.get()).isEqualTo("Current");
    }

    @Test
    public void givenUnknownCrn_whenGetOffenderProbationStatus_thenMakeReturnEmpty() {

        String optional = restClient.getOffenderProbationStatus("X500").block();

        assertThat(optional).isNull();
    }
}
