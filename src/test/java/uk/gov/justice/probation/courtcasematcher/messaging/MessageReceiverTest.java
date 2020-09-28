package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

import static org.mockito.Mockito.verify;

@DisplayName("MessageReceiver is component to receive Libra feeds")
@ExtendWith(MockitoExtension.class)
class MessageReceiverTest {

    @Mock
    private MessageProcessor messageProcessor;

    @Mock
    private TelemetryService telemetryService;

    @InjectMocks
    private MessageReceiver messageReceiver;

    @Test
    void whenMessageReceived_ThenProcess() {
        String msg = "message";

        messageReceiver.receive(msg);

        verify(messageProcessor).process(msg);
        verify(telemetryService).trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
    }
}
