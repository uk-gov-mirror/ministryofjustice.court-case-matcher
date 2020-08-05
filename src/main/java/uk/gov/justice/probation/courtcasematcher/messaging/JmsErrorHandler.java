package uk.gov.justice.probation.courtcasematcher.messaging;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class JmsErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("Unexpected error processing message", throwable.getCause());
        Optional.ofNullable(message).ifPresent(msg -> {
            log.error("Source message {}", message.replaceAll("(?s)<def_name[^>]*>.*?</def_name>",
                "<def_name>*****</def_name>"));
        });
    }

}
