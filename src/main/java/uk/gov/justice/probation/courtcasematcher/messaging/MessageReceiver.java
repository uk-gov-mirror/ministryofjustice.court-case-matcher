package uk.gov.justice.probation.courtcasematcher.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import uk.gov.justice.probation.courtcasematcher.event.CourtCaseFailureEvent;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.ExternalDocumentRequest;

import javax.validation.ConstraintViolationException;

public interface MessageReceiver {

    ExternalDocumentRequest parse(String message) throws JsonProcessingException;

    default void process(String message, String messageId) {
        try {
            getMessageProcessor().process(parse(message), messageId);
        }
        catch (Exception ex) {
            CourtCaseFailureEvent.CourtCaseFailureEventBuilder builder = CourtCaseFailureEvent.builder()
                .failureMessage(ex.getMessage())
                .throwable(ex)
                .incomingMessage(message);
            if (ex instanceof ConstraintViolationException) {
                builder.violations(((ConstraintViolationException)ex).getConstraintViolations());
            }
            getEventBus().post(builder.build());
            throw new RuntimeException(message, ex);
        }
    }

    MessageProcessor getMessageProcessor();

    @SuppressWarnings("UnstableApiUsage")
    EventBus getEventBus();
}
