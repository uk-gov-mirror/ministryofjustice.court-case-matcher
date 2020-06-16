package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcasematcher.application.CaseMapperReference;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;
import uk.gov.justice.probation.courtcasematcher.model.mapper.CaseMapper;
import uk.gov.justice.probation.courtcasematcher.restclient.CourtCaseRestClient;
import uk.gov.justice.probation.courtcasematcher.service.MatcherService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Message Processor")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static final String COURT_CODE = "SHF";
    private static final long REST_CLIENT_WAIT_MS = 2000;
    public static final String CASE_NO = "1600032952";

    private static GatewayMessageParser parser;

    private static final CaseMapperReference caseMapperReference = new CaseMapperReference();

    @Mock
    private EventBus eventBus;

    @Mock
    private CourtCaseRestClient courtCaseRestClient;

    @Mock
    private CaseMapper caseMapper;

    @InjectMocks
    private MatcherService matcherService;

    private MessageProcessor messageProcessor;

    @BeforeAll
    static void beforeAll() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        caseMapperReference.setDefaultProbationStatus("No record");
        caseMapperReference.setCourtNameToCodes(Map.of("SheffieldMagistratesCourt", COURT_CODE));
        parser = new GatewayMessageParser(new XmlMapper(xmlModule));
    }

    @BeforeEach
    void beforeEach() {
        messageProcessor = new MessageProcessor(parser, eventBus, matcherService);
    }

    @DisplayName("Receive a case which matches (by court code and case no) one in court case service. Merge it and PUT.")
    @Test
    void whenCorrectMessageReceived_ForExistingCase_ThenEventsPublished() throws IOException {

        Disposable disposable = Mockito.mock(Disposable.class);
        String path = "src/test/resources/messages/gateway-message-single-case.xml";

        CourtCase existingCourtCase = Mockito.mock(CourtCase.class);
        when(existingCourtCase.getCaseNo()).thenReturn(CASE_NO);
        when(existingCourtCase.getCourtCode()).thenReturn(COURT_CODE);

        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.just(existingCourtCase));
        when(caseMapper.merge(any(Case.class), eq(existingCourtCase))).thenReturn(existingCourtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(existingCourtCase))).thenReturn(disposable);

        messageProcessor.processAll(Files.readString(Paths.get(path)));

        verify(caseMapper).merge(any(Case.class), eq(existingCourtCase));
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(existingCourtCase));
    }

    @DisplayName("Receive a case which does NOT match (by court code and case no) to one in court case service. Attempt to match in offender search. Create new and PUT.")
    @Test
    void whenCorrectMessageReceived_ForNewCase_ThenEventsPublished() throws IOException {

        Disposable disposable = Mockito.mock(Disposable.class);
        String path = "src/test/resources/messages/gateway-message-single-case.xml";

        CourtCase courtCase = Mockito.mock(CourtCase.class);
        when(courtCase.getCaseNo()).thenReturn(CASE_NO);
        when(courtCase.getCourtCode()).thenReturn(COURT_CODE);

        when(courtCaseRestClient.getCourtCase(COURT_CODE, CASE_NO)).thenReturn(Mono.empty());
        when(caseMapper.newFromCase(any(Case.class))).thenReturn(courtCase);
        when(courtCaseRestClient.putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase))).thenReturn(disposable);

        messageProcessor.processAll(Files.readString(Paths.get(path)));

        verify(caseMapper).newFromCase(any(Case.class));
        verify(courtCaseRestClient, timeout(REST_CLIENT_WAIT_MS)).putCourtCase(eq(COURT_CODE), eq(CASE_NO), eq(courtCase));
    }

    @DisplayName("An XML message which is invalid")
    @Test
    void whenInvalidMessageReceived_NothingPublished() {

        messageProcessor.processAll("<someOtherXml>Not the message you are looking for</someOtherXml>");

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }

    @DisplayName("A valid message but with 0 cases")
    @Test
    void whenCorrectMessageWithZeroCasesReceived_ThenNoEventsPublished() throws IOException {

        String path = "src/test/resources/messages/gateway-message-empty-sessions.xml";

        messageProcessor.processAll(Files.readString(Paths.get(path)));

        verify(eventBus).post(any(CourtCaseFailureEvent.class));
    }

    private static class CaseMatcher implements ArgumentMatcher<Case> {

        private final Long id;

        public CaseMatcher(Long id) {
            this.id = id;
        }

        @Override
        public boolean matches(Case aCase) {
            return id.equals(aCase.getId());
        }
    }

}
