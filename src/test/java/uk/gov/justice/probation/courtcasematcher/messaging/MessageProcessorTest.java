package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.eventbus.EventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Case;

@DisplayName("Tests processing of messages")
@ExtendWith(MockitoExtension.class)
class MessageProcessorTest {

    private static GatewayMessageParser parser;

    @Mock
    private EventBus eventBus;

    private MessageProcessor messageProcessor;

    @BeforeAll
    static void beforeAll() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        parser = new GatewayMessageParser(new XmlMapper(xmlModule));
    }

    @BeforeEach
    void beforeEach() {
        messageProcessor = new MessageProcessor(parser, eventBus);
    }

    @DisplayName("A message with 2 cases")
    @Test
    void whenCorrectMessageWithTwoCasesReceived_ThenTwoEventsPublished() throws IOException {

        String path = "src/test/resources/messages/gateway-message-simple.xml";

        messageProcessor.process(Files.readString(Paths.get(path)));

        verify(eventBus).post(argThat(new CaseMatcher(1217464L)));
        verify(eventBus).post(argThat(new CaseMatcher(1217465L)));
    }

    @DisplayName("An XML message which is invalid")
    @Test
    void whenInvalidMessageReceived_NothingPublished() {

        messageProcessor.process("<someOtherXml>Not the message you are looking for</someOtherXml>");

        verify(eventBus, never()).post(any(Case.class));
    }

    @DisplayName("A valid message but with 0 cases")
    @Test
    void whenCorrectMessageWithZeroCasesReceived_ThenNoEventsPublished() throws IOException {

        String path = "src/test/resources/messages/gateway-message-empty-sessions.xml";

        messageProcessor.process(Files.readString(Paths.get(path)));

        verify(eventBus, never()).post(any(Case.class));
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
