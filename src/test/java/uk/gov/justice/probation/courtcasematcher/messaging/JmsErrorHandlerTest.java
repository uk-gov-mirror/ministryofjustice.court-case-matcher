package uk.gov.justice.probation.courtcasematcher.messaging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.eventbus.EventBus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.event.EventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class JmsErrorHandlerTest {

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @BeforeEach
    void beforeEach() {
        Logger logger = (Logger) getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(mockAppender);
    }

    @Test
    void whenHandleError_ThenRemoveDefName() throws IOException {
        String path = "src/test/resources/messages/gateway-message-multi-day.xml";
        String content = Files.readString(Paths.get(path));

        new JmsErrorHandler().handleError(new RuntimeException(content));

        verify(mockAppender, atLeast(1)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> events = captorLoggingEvent.getAllValues();
        assertThat(events).hasSizeGreaterThanOrEqualTo(1);
        events.forEach(loggingEvent -> {
            assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
            assertThat(loggingEvent.getFormattedMessage()).doesNotContain("<def_name>Mr David WATTS</def_name>");
        });
    }
}
