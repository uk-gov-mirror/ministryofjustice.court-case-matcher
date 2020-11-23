package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsMessageReceiverTest {

    @Mock
    private MessageProcessor messageProcessor;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private EventBus eventBus;

    @Mock
    private GatewayMessageParser<ExternalDocumentRequest> parser;

    @InjectMocks
    private SqsMessageReceiver messageReceiver;

    @DisplayName("Given a valid message then track and process")
    @Test
    void givenValidMessage_whenReceived_ThenTrackAndProcess() throws JsonProcessingException {
        ExternalDocumentRequest externalDocumentRequest = ExternalDocumentRequest.builder().build();
        when(parser.parseMessage("message", ExternalDocumentRequest.class)).thenReturn(externalDocumentRequest);

        messageReceiver.receive("message", "MessageID");

        verify(messageProcessor).process(externalDocumentRequest);
        verify(telemetryService).trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
    }

    @DisplayName("Given a valid message then track and process")
    @Test
    void givenInvalidMessage_whenReceived_ThenTrackAndEventFail() throws JsonProcessingException {
        when(parser.parseMessage("message", ExternalDocumentRequest.class)).thenThrow(JsonProcessingException.class);

        try {
            messageReceiver.receive("message", "MessageID");
            fail("Expected a RuntimeException");
        }
        catch (RuntimeException ex) {
            verify(telemetryService).trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
            verify(eventBus).post(any(CourtCaseFailureEvent.class));
        }
    }

}
