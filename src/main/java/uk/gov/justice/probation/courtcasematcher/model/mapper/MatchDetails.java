package uk.gov.justice.probation.courtcasematcher.model.mapper;

import lombok.Builder;
import lombok.Getter;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Match;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;

import java.util.List;

@Builder
@Getter
public class MatchDetails {
    private final MatchType matchType;
    private final List<Match> matches;
    private final String probationStatus;
    private final boolean exactMatch;
}
