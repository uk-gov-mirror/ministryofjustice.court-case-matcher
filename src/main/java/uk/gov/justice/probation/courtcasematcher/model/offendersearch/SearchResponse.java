package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class SearchResponse {
    private final List<Match> matches;
    private final MatchType matchedBy;
}
