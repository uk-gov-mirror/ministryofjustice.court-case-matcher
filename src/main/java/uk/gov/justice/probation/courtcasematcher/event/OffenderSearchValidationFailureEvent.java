package uk.gov.justice.probation.courtcasematcher.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class OffenderSearchValidationFailureEvent {
    private String failureMessage;
    private String fullName;
    private LocalDate dateOfBirth;
}
