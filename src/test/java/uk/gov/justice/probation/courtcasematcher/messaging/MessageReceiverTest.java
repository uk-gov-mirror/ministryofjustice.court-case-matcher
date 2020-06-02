package uk.gov.justice.probation.courtcasematcher.messaging;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
