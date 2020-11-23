package uk.gov.justice.probation.courtcasematcher.messaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.eventbus.EventBus;
import javax.validation.Validator;
import lombok.Builder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseMatchEvent;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseUpdateEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Info;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.service.CourtCaseService;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.DefendantType.ORGANISATION;
import static uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.DefendantType.PERSON;

@DisplayName("Message Processor")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final long MATCHER_THREAD_TIMEOUT = 4000;
    private static final String COURT_CODE_00 = "B01CX00";
    private static final String COURT_CODE_02 = "B01CX02";
    private static String singleCaseXml;
    private static String multiSessionXml;
    private static String multiDayXml;

    @Mock
    private Validator validator;

    @Mock
    private EventBus eventBus;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private CourtCaseService courtCaseService;

    @InjectMocks
    private MessageProcessor messageProcessor;
    private GatewayMessageParser<ExternalDocumentRequest> parser;

    @BeforeAll
    static void beforeAll() throws IOException {

        String multipleSessionPath = "src/test/resources/messages/external-document-request-multi-session.xml";
        multiSessionXml = Files.readString(Paths.get(multipleSessionPath));

        singleCaseXml = Files.readString(Paths.get("src/test/resources/messages/external-document-request-single-case.xml"));

        String multiDayPath = "src/test/resources/messages/external-document-request-multi-day.xml";
        multiDayXml = Files.readString(Paths.get(multiDayPath));
    }

//    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        parser = new GatewayMessageParser<>(new XmlMapper(xmlModule), validator);
    }

    @DisplayName("Receive a valid case which exists then post to the event bus")
    @Test
    void whenValidMessageReceived_ThenAttemptMatch() throws JsonProcessingException {

        Name name = Name.builder().forename1("Donald").surname("TRUMP").build();
        CourtCase courtCase =  CourtCase.builder().isNew(false).name(name).build();
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        ExternalDocumentRequest documentRequest = parser.parseMessage(singleCaseXml, ExternalDocumentRequest.class);

        messageProcessor.process(documentRequest);

        verify(eventBus, timeout(1000)).post(any(CourtCaseUpdateEvent.class));
        verify(telemetryService).trackCourtListEvent(any(Info.class));
        verify(telemetryService).trackCourtCaseEvent(any(Case.class));
        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("Receive multiple valid cases then attempt to match two and update two")
    @Test
    void givenTwoExistingAndOneNew_whenValidMessageReceivedWithMultipleSessions_ThenAttemptMatchOnOneAndUpdateOnTwo() throws JsonProcessingException {

        CaseMatcher case1 = CaseMatcher.builder().caseNo("1600032953").build();
        CourtCase courtCase1 = CourtCase.builder().isNew(false).caseNo("1600032953").defendantType(PERSON).build();
        CaseMatcher case2 = CaseMatcher.builder().caseNo("1600032979").build();
        CourtCase courtCase2 = CourtCase.builder().isNew(false).caseNo("1600032979").defendantType(PERSON).build();
        CaseMatcher case3 = CaseMatcher.builder().caseNo("1600032952").build();
        CourtCase courtCase3 = CourtCase.builder().isNew(true).caseNo("1600032952").defendantType(PERSON).build();
        CaseMatcher case4 = CaseMatcher.builder().caseNo("1600011111").build();
        CourtCase courtCase4 = CourtCase.builder().isNew(true).caseNo("1600011111").defendantType(ORGANISATION).build();

        when(courtCaseService.getCourtCase(argThat(case1))).thenReturn(Mono.just(courtCase1));
        when(courtCaseService.getCourtCase(argThat(case2))).thenReturn(Mono.just(courtCase2));
        when(courtCaseService.getCourtCase(argThat(case3))).thenReturn(Mono.just(courtCase3));
        when(courtCaseService.getCourtCase(argThat(case4))).thenReturn(Mono.just(courtCase4));

        ExternalDocumentRequest documentRequest = parser.parseMessage(multiSessionXml, ExternalDocumentRequest.class);
        messageProcessor.process(documentRequest);

        // 2 new cases get matcher events. The organisation does not get a matching event despite the fact it is new
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600032953").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600032979").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseMatchEventMatcher.builder().caseNo("1600032952").build()));
        verify(eventBus, timeout(1000)).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1600011111").build()));
        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.FEBRUARY, 20), COURT_CODE_00)));
        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.FEBRUARY, 23), COURT_CODE_02)));
        verify(telemetryService).trackCourtCaseEvent(argThat(case1));
        verify(telemetryService).trackCourtCaseEvent(argThat(case2));
        verify(telemetryService).trackCourtCaseEvent(argThat(case3));
        verify(telemetryService).trackCourtCaseEvent(argThat(case4));

        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("Receive multiple valid cases then attempt post as updates.")
    @Test
    void whenValidMessageReceivedWithMultipleDays_ThenAttemptMatch() throws JsonProcessingException {
        CourtCase courtCase = mock(CourtCase.class);
        when(courtCase.isNew()).thenReturn(Boolean.FALSE);
        when(courtCaseService.getCourtCase(any(Case.class))).thenReturn(Mono.just(courtCase));

        ExternalDocumentRequest documentRequest = parser.parseMessage(multiDayXml, ExternalDocumentRequest.class);
        messageProcessor.process(documentRequest);

        verify(eventBus, timeout(MATCHER_THREAD_TIMEOUT).times(6)).post(any(CourtCaseUpdateEvent.class));

        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.JULY, 25))));
        verify(telemetryService).trackCourtListEvent(argThat(matchesInfoWith(LocalDate.of(2020, Month.JULY, 28))));
        verify(telemetryService, times(6)).trackCourtCaseEvent(any(Case.class));
        verifyNoMoreInteractions(eventBus, telemetryService);
    }

    @DisplayName("A valid message but with 0 cases")
    @Test
    void whenCorrectMessageWithZeroCasesReceived_ThenNoEventsPublished() throws IOException {

        String path = "src/test/resources/messages/external-document-request-empty-sessions.xml";
        ExternalDocumentRequest documentRequest = parser.parseMessage(Files.readString(Paths.get(path)), ExternalDocumentRequest.class);

        messageProcessor.process(documentRequest);

        verify(eventBus, never()).post(any(CourtCaseFailureEvent.class));
    }

    @DisplayName("An existing case based on a person does not match")
    @Test
    void givenExistingPersonCase_whenPostCase_thenDoNotMatch() {

        CourtCase courtCase = CourtCase.builder().isNew(false).caseNo("1").defendantType(PERSON).build();

        messageProcessor.postCaseEvent(courtCase);

        verify(eventBus).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1").build()));
        verifyNoMoreInteractions(eventBus);
    }

    @DisplayName("A new case based on a person does not match")
    @Test
    void givenNewPersonCase_whenPostCase_thenMatch() {

        CourtCase courtCase = CourtCase.builder().isNew(true).caseNo("1").defendantType(PERSON).build();

        messageProcessor.postCaseEvent(courtCase);

        verify(eventBus).post(argThat(CourtCaseMatchEventMatcher.builder().caseNo("1").build()));
        verifyNoMoreInteractions(eventBus);
    }

    @DisplayName("A new case based on an organisation does not match")
    @Test
    void givenNewOrganisationCase_whenPostCase_thenDoNotMatch() {

        CourtCase courtCase = CourtCase.builder().isNew(true).caseNo("1").defendantType(ORGANISATION).build();

        messageProcessor.postCaseEvent(courtCase);

        verify(eventBus).post(argThat(CourtCaseUpdateEventMatcher.builder().caseNo("1").build()));
        verifyNoMoreInteractions(eventBus);
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

    @Builder
    public static class InfoMatcher implements ArgumentMatcher<Info> {

        private final LocalDate dateOfHearing;
        private final String courtCode;

        @Override
        public boolean matches(Info otherInfo) {
            return otherInfo != null
                && dateOfHearing.equals(otherInfo.getDateOfHearing())
                && courtCode.equalsIgnoreCase(otherInfo.getInfoSourceDetail().getOuCode());
        }
    }

    private InfoMatcher matchesInfoWith(LocalDate dateOfHearing, String courtCode) {
        return InfoMatcher.builder()
            .courtCode(courtCode)
            .dateOfHearing(dateOfHearing)
            .build();
    }

    private InfoMatcher matchesInfoWith(LocalDate dateOfHearing) {
        return matchesInfoWith(dateOfHearing, COURT_CODE_00);
    }
}
