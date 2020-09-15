package uk.gov.justice.probation.courtcasematcher.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.CourtCase;

@Getter
@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class CourtCaseMatchEvent {

    private final CourtCase courtCase;
}
