package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Deprecated(forRemoval = true)
// TODO - this object can go when the matcher no longer needs to go to court case service for probation status detail
public class ProbationStatusDetail {
    private final String probationStatus;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean inBreach;
    private final boolean preSentenceActivity;
}
