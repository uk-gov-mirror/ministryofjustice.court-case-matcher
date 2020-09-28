package uk.gov.justice.probation.courtcasematcher.messaging;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryEventType;
import uk.gov.justice.probation.courtcasematcher.service.TelemetryService;

@Slf4j
@Service
@AllArgsConstructor
public class MessageReceiver {

    private static final String CP_QUEUE = "CP_OutboundQueue";

    private final MessageProcessor messageProcessor;

    private final TelemetryService telemetryService;

    @JmsListener(destination = CP_QUEUE)
    public void receive(String message) {
        log.info("Received message");
        telemetryService.trackEvent(TelemetryEventType.COURT_LIST_MESSAGE_RECEIVED);
        try {
            messageProcessor.process(message);
        }
        catch (Exception exception) {
            throw new RuntimeException(message, exception);
        }
    }

}
