package uk.gov.justice.probation.courtcasematcher.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

@Data
@Builder
public class SearchResult {
    private SearchResponse searchResponse;
    private MatchRequest matchRequest;
}
