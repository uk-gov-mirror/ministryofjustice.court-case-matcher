package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.eventbus.EventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

@DisplayName("Message Processor")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final String COURT_CODE = "SHF";
    public static final String CASE_NO = "1600032952";

    private static final long MATCHER_THREAD_TIMEOUT = 4000;
    private static final CaseMapperReference caseMapperReference = new CaseMapperReference();
    private static String singleCaseXml;
    private static String multiSessionXml;
    private static String multiDayXml;
    private static String singleDayXml;
    private static String singleDayFutureXml;
    private static String multiCourtXml;

    @Mock
    private EventBus eventBus;

    @Mock
    private MatcherService matcherService;

    @Mock
    private CourtCaseService courtCaseService;

    @Mock
    private Validator validator;

    private MessageProcessor messageProcessor;

    @Captor
    private ArgumentCaptor<Case> captor;

    @Captor
    private ArgumentCaptor<List<Case>> captorCases;

    @BeforeAll
    static void beforeAll() throws IOException {

        String multipleSessionPath = "src/test/resources/messages/gateway-message-multi-session.xml";
        multiSessionXml = Files.readString(Paths.get(multipleSessionPath));

        String path = "src/test/resources/messages/gateway-message-single-case.xml";
        singleCaseXml = Files.readString(Paths.get(path));

        String multiDayPath = "src/test/resources/messages/gateway-message-multi-day.xml";
        multiDayXml = Files.readString(Paths.get(multiDayPath));

        String singleDayPath = "src/test/resources/messages/gateway-message-single-day.xml";
        singleDayXml = Files.readString(Paths.get(singleDayPath));
        singleDayXml = singleDayXml.replace("[TODAY]", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String singleDayFuturePath = "src/test/resources/messages/gateway-message-single-day-future.xml";
        singleDayFutureXml = Files.readString(Paths.get(singleDayFuturePath));
        singleDayFutureXml = singleDayFutureXml.replace("[FUTURE]", LocalDate.now().plusDays(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String multiCourtPath = "src/test/resources/messages/gateway-message-multi-court.xml";
        multiCourtXml = Files.readString(Paths.get(multiCourtPath));
        multiCourtXml = multiCourtXml.replace("[TODAY]", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        caseMapperReference.setDefaultProbationStatus("No record");
        caseMapperReference.setCourtNameToCodes(Map.of("SheffieldMagistratesCourt", COURT_CODE, "Beverley", "BEV"));
    }

    @BeforeEach
    void beforeEach() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        GatewayMessageParser parser = new GatewayMessageParser(new XmlMapper(xmlModule), validator);
        messageProcessor = new MessageProcessor(parser, eventBus, matcherService, courtCaseService);
        messageProcessor.setCaseFeedFutureDateOffset(3);
    }

    @DisplayName("Receive a valid case then attempt to match")
    @Test
    void whenValidMessageReceived_ThenAttemptMatch() {

        messageProcessor.process(singleCaseXml);

        verify(matcherService).match(captor.capture());
        assertThat(captor.getValue().getCaseNo()).isEqualTo(CASE_NO);
        assertThat(captor.getValue().getBlock().getSession().getCourtCode()).isEqualTo(COURT_CODE);
    }

    @DisplayName("Receive multiple valid cases then attempt to match")
    @Test
    void whenValidMessageReceivedWithMultipleSessions_ThenAttemptMatch() {

        LocalDate expectedDate1 = LocalDate.of(2020, Month.FEBRUARY, 20);
        LocalDate expectedDate2 = LocalDate.of(2020, Month.FEBRUARY, 23);

        messageProcessor.process(multiSessionXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(3)).match(any(Case.class));
        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).purgeAbsent(eq(COURT_CODE), eq(Set.of(expectedDate1, expectedDate2)), captorCases.capture());

        assertThat(captorCases.getValue().size()).isEqualTo(3);
    }

    @DisplayName("Receive multiple valid cases then attempt to match")
    @Test
    void whenValidMessageReceivedWithMultipleDays_ThenAttemptMatch() {

        LocalDate date1 = LocalDate.of(2020, Month.JULY, 25);
        LocalDate date2 = LocalDate.of(2020, Month.JULY, 28);

        messageProcessor.process(multiDayXml);

        InOrder inOrder = inOrder(matcherService, courtCaseService);
        inOrder.verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(6)).match(any(Case.class));
        inOrder.verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).purgeAbsent(eq(COURT_CODE), eq(Set.of(date1, date2)), captorCases.capture());

        assertThat(captorCases.getValue().size()).isEqualTo(6);
    }

    @DisplayName("Receive only a single day (document) for cases today. Need to purge for the second date.")
    @Test
    void whenValidMessageReceivedWithSingleDay_ThenPurgeForSecondExpected() {

        LocalDate date1 = LocalDate.now();
        LocalDate date2 = date1.plusDays(3);

        messageProcessor.process(singleDayXml);

        InOrder inOrder = inOrder(matcherService, courtCaseService);
        inOrder.verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(2)).match(any(Case.class));
        inOrder.verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).purgeAbsent(eq(COURT_CODE), eq(Set.of(date1, date2)), captorCases.capture());

        assertThat(captorCases.getValue().size()).isEqualTo(2);
    }

    @DisplayName("Receive only a single day (document) for cases in 3 days time. None for today. We need to purge for today's cases.")
    @Test
    void whenValidMessageReceivedWithSingleDayNoCasesToday_ThenPurgeForTodayExpected() {

        LocalDate date1 = LocalDate.now();
        LocalDate date2 = date1.plusDays(3);

        messageProcessor.process(singleDayFutureXml);

        InOrder inOrder = inOrder(matcherService, courtCaseService);
        inOrder.verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(1)).match(any(Case.class));
        inOrder.verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).purgeAbsent(eq(COURT_CODE), eq(Set.of(date1, date2)), captorCases.capture());

        assertThat(captorCases.getValue().size()).isEqualTo(1);
    }

    @DisplayName("")
    @Test
    void whenValidMessageReceivedWithMultipleCourts_ThenPurgeForBoth() {

        LocalDate date1 = LocalDate.now();
        LocalDate date2 = date1.plusDays(3);

        messageProcessor.process(multiCourtXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(2)).match(any(Case.class));
        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).purgeAbsent(eq(COURT_CODE), eq(Set.of(date1, date2)), captorCases.capture());
        List<Case> shfCases = captorCases.getValue();
        assertThat(shfCases.size()).isEqualTo(1);
        assertThat(shfCases.get(0).getCaseNo()).isEqualTo("1000000005");

        verify(courtCaseService, timeout(MATCHER_THREAD_TIMEOUT)).purgeAbsent(eq("BEV"), eq(Set.of(date1, date2)), captorCases.capture());
        assertThat(captorCases.getValue().size()).isEqualTo(1);
        List<Case> bevCases = captorCases.getValue();
        assertThat(bevCases.size()).isEqualTo(1);
        assertThat(bevCases.get(0).getCaseNo()).isEqualTo("1000000001");
    }


    @DisplayName("An XML message which is invalid")
    @Test
    void whenInvalidXmlMessageReceived_NothingPublished() {

        messageProcessor.process("<someOtherXml>Not the message you are looking for</someOtherXml>");

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }

    @DisplayName("An XML message which is grammatically valid but fails validation")
    @Test
    void whenInvalidMessageReceived_NothingPublished() {
        @SuppressWarnings("unchecked") ConstraintViolation<MessageType> cv = mock(ConstraintViolation.class);
        when(validator.validate(any(MessageType.class))).thenReturn(Set.of(cv));

        messageProcessor.process(singleCaseXml);

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }

    @DisplayName("A valid message but with 0 cases")
    @Test
    void whenCorrectMessageWithZeroCasesReceived_ThenNoEventsPublished() throws IOException {

        String path = "src/test/resources/messages/gateway-message-empty-sessions.xml";

        messageProcessor.process(Files.readString(Paths.get(path)));

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }

}
