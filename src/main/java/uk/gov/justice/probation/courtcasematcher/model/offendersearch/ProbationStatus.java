package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Builder
@Data
public class ProbationStatus {
    private final String status;
    private final boolean preSentenceActivity;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean inBreach;
}
