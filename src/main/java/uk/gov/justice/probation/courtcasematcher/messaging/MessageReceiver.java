package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageReceiver {

    private static final String CP_QUEUE = "CP_OutboundQueue";

    private final MessageProcessor messageProcessor;

    public MessageReceiver (MessageProcessor processor) {
        super();
        this.messageProcessor = processor;
    }

    @JmsListener(destination = CP_QUEUE)
    public void receive(String message) {
        log.info("Received message");
        log.trace("Raw message contents for parsing:{}", message);
        try {
            messageProcessor.process(message);
        }
        catch (Exception exception) {
            throw new RuntimeException(message, exception);
        }
    }

}
