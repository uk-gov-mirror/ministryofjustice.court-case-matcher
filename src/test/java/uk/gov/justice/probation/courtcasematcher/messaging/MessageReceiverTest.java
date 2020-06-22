package uk.gov.justice.probation.courtcasematcher.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@DisplayName("MessageReceiver is component to receive ")
@ExtendWith(MockitoExtension.class)
class MessageReceiverTest {

    @Mock
    private MessageProcessor messageProcessor;

    @InjectMocks
    private MessageReceiver messageReceiver;

    @Test
    void whenMessageReceived_ThenProcess() {
        String msg = "message";

        messageReceiver.receive(msg);

        verify(messageProcessor).process(msg);
    }
}
