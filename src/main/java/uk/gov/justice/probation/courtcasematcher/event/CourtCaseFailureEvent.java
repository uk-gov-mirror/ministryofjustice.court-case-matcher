package uk.gov.justice.probation.courtcasematcher.event;

import java.util.Set;
import javax.validation.ConstraintViolation;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Builder
public class CourtCaseFailureEvent {

    private final String incomingMessage;

    private final String failureMessage;

    private final Set<ConstraintViolation<?>> violations;

}
