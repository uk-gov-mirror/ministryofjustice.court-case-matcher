package uk.gov.justice.probation.courtcasematcher.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class OffenderSearchValidationFailureEvent {
    private final  String failureMessage;
    private final String fullName;
    private final LocalDate dateOfBirth;
}
