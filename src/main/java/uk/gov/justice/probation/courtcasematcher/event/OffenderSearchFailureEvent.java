package uk.gov.justice.probation.courtcasematcher.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;

@AllArgsConstructor
@Getter
@Builder
public class OffenderSearchFailureEvent {
    private final String failureMessage;
    private final CourtCase courtCase;
}
