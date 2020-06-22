package uk.gov.justice.probation.courtcasematcher.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OffenderSearchFailureEvent {
    private final String requestJson;
    private final String failureMessage;
}
