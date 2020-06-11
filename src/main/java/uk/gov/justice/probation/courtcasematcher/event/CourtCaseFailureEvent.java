package uk.gov.justice.probation.courtcasematcher.event;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Builder
public class CourtCaseFailureEvent {

    private final String incomingMessage;

    private final String failureMessage;

}
