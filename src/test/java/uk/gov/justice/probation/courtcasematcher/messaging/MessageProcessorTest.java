package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.eventbus.EventBus;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import lombok.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseMatchEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseUpdateEvent;
import uk.gov.justice.probation.courtcasematcher.model.MessageType;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Message Processor")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final long MATCHER_THREAD_TIMEOUT = 4000;
    private static String singleCaseXml;
    private static String multiSessionXml;
    private static String multiDayXml;

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

    @BeforeAll
    static void beforeAll() throws IOException {

        String multipleSessionPath = "src/test/resources/messages/gateway-message-multi-session.xml";
        multiSessionXml = Files.readString(Paths.get(multipleSessionPath));

        String path = "src/test/resources/messages/gateway-message-single-case.xml";
        singleCaseXml = Files.readString(Paths.get(path));

        String multiDayPath = "src/test/resources/messages/gateway-message-multi-day.xml";
        multiDayXml = Files.readString(Paths.get(multiDayPath));

        String singleDayPath = "src/test/resources/messages/gateway-message-single-day.xml";
        String singleDayXml = Files.readString(Paths.get(singleDayPath));
        singleDayXml = singleDayXml.replace("[TODAY]", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String singleDayFuturePath = "src/test/resources/messages/gateway-message-single-day-future.xml";
        String singleDayFutureXml = Files.readString(Paths.get(singleDayFuturePath));
        singleDayFutureXml = singleDayFutureXml.replace("[FUTURE]", LocalDate.now().plusDays(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        String multiCourtPath = "src/test/resources/messages/gateway-message-multi-court.xml";
        String multiCourtXml = Files.readString(Paths.get(multiCourtPath));
        multiCourtXml = multiCourtXml.replace("[TODAY]", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

    }

    @BeforeEach
    void beforeEach() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        GatewayMessageParser parser = new GatewayMessageParser(new XmlMapper(xmlModule), validator);
        messageProcessor = new MessageProcessor(parser, eventBus, matcherService, courtCaseService);
        messageProcessor.setCaseFeedFutureDateOffset(3);
    }

    @DisplayName("Receive a valid case which exists then post to the event bus")
    @Test
    void whenValidMessageReceived_ThenAttemptMatch() {

        CourtCase courtCase =  CourtCase.builder().isNew(false).defendantName("NAME").build();
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        messageProcessor.process(singleCaseXml);

        verify(eventBus, timeout(1000)).post(any(CourtCaseUpdateEvent.class));
    }

    @DisplayName("Receive multiple valid cases then attempt to match one and update two")
    @Test
    void givenTwoExistingAndOneNew_whenValidMessageReceivedWithMultipleSessions_ThenAttemptMatchOnOneAndUpdateOnTwo() {

        CaseMatcher case1 = CaseMatcher.builder().caseNo("1600032953").build();
        CourtCase courtCase1 = CourtCase.builder().isNew(false).caseNo("1600032953").build();
        CaseMatcher case2 = CaseMatcher.builder().caseNo("1600032979").build();
        CourtCase courtCase2 = CourtCase.builder().isNew(false).caseNo("1600032979").build();
        CaseMatcher case3 = CaseMatcher.builder().caseNo("1600032952").build();
        CourtCase courtCase3 = CourtCase.builder().isNew(true).caseNo("1600032952").build();

        when(courtCaseService.getCourtCase(argThat(case1))).thenReturn(Mono.just(courtCase1));
        when(courtCaseService.getCourtCase(argThat(case2))).thenReturn(Mono.just(courtCase2));
        when(courtCaseService.getCourtCase(argThat(case3))).thenReturn(Mono.just(courtCase3));

        messageProcessor.process(multiSessionXml);

        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600032953").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600032979").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseMatchEventMatcher.builder().caseNo("1600032952").build()));
    }

    @DisplayName("Receive multiple valid cases then attempt post as updates.")
    @Test
    void whenValidMessageReceivedWithMultipleDays_ThenAttemptMatch() {
        CourtCase courtCase = mock(CourtCase.class);
        when(courtCase.isNew()).thenReturn(Boolean.FALSE);
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        messageProcessor.process(multiDayXml);

        verify(eventBus, timeout(MATCHER_THREAD_TIMEOUT).times(6)).post(any(CourtCaseUpdateEvent.class));
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


    @Builder
    public static class CourtCaseUpdateEventMatcher implements ArgumentMatcher<CourtCaseUpdateEvent> {

        private final String caseNo;

        @Override
        public boolean matches(CourtCaseUpdateEvent otherCase) {
            return otherCase != null && caseNo.equals(otherCase.getCourtCase().getCaseNo());
        }
    }

    @Builder
    public static class CourtCaseMatchEventMatcher implements ArgumentMatcher<CourtCaseMatchEvent> {

        private final String caseNo;

        @Override
        public boolean matches(CourtCaseMatchEvent otherCase) {
            return otherCase != null && caseNo.equals(otherCase.getCourtCase().getCaseNo());
        }
    }

    @Builder
    public static class CaseMatcher implements ArgumentMatcher<Case> {

        private final String caseNo;

        @Override
        public boolean matches(Case otherCase) {
            return otherCase != null && caseNo.equals(otherCase.getCaseNo());
        }
    }
}
