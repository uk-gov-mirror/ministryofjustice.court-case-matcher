package uk.gov.justice.probation.courtcasematcher.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.eventbus.EventBus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;

class EventListenerTest {

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    private EventBus eventBus;

    private Logger logger;

    private EventListener eventListener;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.initMocks(this);
        logger = (Logger) getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(mockAppender);

        eventBus = new EventBus();
        eventListener = new EventListener(eventBus);
    }

    @DisplayName("Ensure that successful events are logged and counted")
    @Test
    void testSuccessEvent() {
        CourtCase courtCaseApi = CourtCase.builder().caseNo("123").courtCode("SHF").build();

        eventListener.courtCaseEvent(CourtCaseSuccessEvent.builder().courtCaseApi(courtCaseApi).build());

        verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains("EventBus success event for posting case 123 for court SHF. Total count of successful messages 1");
        assertThat(eventListener.getSuccessCount()).isEqualTo(1);
    }

    @DisplayName("Ensure that failure events are logged and counted")
    @Test
    void testFailureEvent() {

        eventListener.courtCaseEvent(CourtCaseFailureEvent.builder().failureMessage("Problem").build());

        verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSize(1);
        LoggingEvent loggingEvent = events.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage().trim())
            .contains("Message processing failed. Current error count: 1");
        assertThat(eventListener.getFailureCount()).isEqualTo(1);
    }

    @DisplayName("Ensure that the EventListener is registered by the EventBus")
    @Test
    void checkEventBusRegistration() {
        eventBus.post(CourtCaseFailureEvent.builder().failureMessage("Problem").build());

        assertThat(eventListener.getFailureCount()).isEqualTo(1);
    }



}