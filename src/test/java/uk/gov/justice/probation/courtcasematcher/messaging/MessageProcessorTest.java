package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.time.format.DateTimeFormatter;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

@DisplayName("Message Processor")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final String COURT_CODE = "B01CX00";
    public static final String CASE_NO = "1600032952";

    private static final long MATCHER_THREAD_TIMEOUT = 4000;
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
    private Validator validator;

    private MessageProcessor messageProcessor;

    @Captor
    private ArgumentCaptor<Case> captor;

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

    }

    @BeforeEach
    void beforeEach() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        GatewayMessageParser parser = new GatewayMessageParser(new XmlMapper(xmlModule), validator);
        messageProcessor = new MessageProcessor(parser, eventBus, matcherService);
        messageProcessor.setCaseFeedFutureDateOffset(3);
    }

    @DisplayName("Receive a valid case then attempt to match")
    @Test
    void whenValidMessageReceived_ThenAttemptMatch() {

        messageProcessor.process(singleCaseXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT)).match(captor.capture());
        assertThat(captor.getValue().getCaseNo()).isEqualTo(CASE_NO);
        assertThat(captor.getValue().getBlock().getSession().getCourtCode()).isEqualTo(COURT_CODE);
    }

    @DisplayName("Receive multiple valid cases then attempt to match")
    @Test
    void whenValidMessageReceivedWithMultipleSessions_ThenAttemptMatch() {

        messageProcessor.process(multiSessionXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(3)).match(any(Case.class));
    }

    @DisplayName("Receive multiple valid cases then attempt to match")
    @Test
    void whenValidMessageReceivedWithMultipleDays_ThenAttemptMatch() {

        messageProcessor.process(multiDayXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(6)).match(any(Case.class));
    }

    @DisplayName("Receive only a single day (document) for cases today. Need to purge for the second date.")
    @Test
    void whenValidMessageReceivedWithSingleDay_ThenPurgeForSecondExpected() {

        messageProcessor.process(singleDayXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(2)).match(any(Case.class));
    }

    @DisplayName("Receive only a single day (document) for cases in 3 days time. None for today. We need to purge for today's cases.")
    @Test
    void whenValidMessageReceivedWithSingleDayNoCasesToday_ThenPurgeForTodayExpected() {

        messageProcessor.process(singleDayFutureXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(1)).match(any(Case.class));
    }

    @DisplayName("Two courts means two calls to purgeAbsent")
    @Test
    void whenValidMessageReceivedWithMultipleCourts_ThenPurgeForBoth() {

        messageProcessor.process(multiCourtXml);

        verify(matcherService, timeout(MATCHER_THREAD_TIMEOUT).times(2)).match(any(Case.class));
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
