package uk.gov.justice.probation.courtcasematcher.event;

import java.util.Set;
import javax.validation.ConstraintViolation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CourtCaseFailureEvent {

    private final String incomingMessage;

    private final Throwable throwable;

    private final String failureMessage;

    private final Set<ConstraintViolation<?>> violations;

}
