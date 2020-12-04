package uk.gov.justice.probation.courtcasematcher.model.mapper;

import lombok.Builder;
import lombok.Data;
import uk.gov.justice.probation.courtcasematcher.model.courtcaseservice.ProbationStatusDetail;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;

import java.util.List;

@Builder
@Data
public class MatchDetails {
    private final MatchType matchType;
    private final List<Match> matches;
    private final ProbationStatusDetail probationStatusDetail;
    private final boolean exactMatch;
}
