package uk.gov.justice.probation.courtcasematcher.event;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Builder
public class OffenderSearchFailureEvent {
    private final String requestJson;
    private final String failureMessage;
}
