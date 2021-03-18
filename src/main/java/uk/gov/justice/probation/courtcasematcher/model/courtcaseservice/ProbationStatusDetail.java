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
public class ProbationStatusDetail {
    private final String status;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean inBreach;
    private final boolean preSentenceActivity;
}
