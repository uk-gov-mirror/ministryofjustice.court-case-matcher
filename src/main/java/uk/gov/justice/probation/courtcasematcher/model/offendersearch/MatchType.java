package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.probation.courtcasematcher.service.SearchResult;

import java.util.Optional;

@Slf4j
public enum MatchType {
    NAME_DOB,
    NAME_DOB_ALIAS,
    NAME,
    PARTIAL_NAME,
    PARTIAL_NAME_DOB_LENIENT,
    NOTHING,
    UNKNOWN,
    NAME_DOB_PNC,
    EXTERNAL_KEY;

    public static MatchType of(SearchResult result) {
        var matchedBy = Optional.ofNullable(result.getSearchResponse())
                .map(SearchResponse::getMatchedBy)
                .orElse(null);

        if (matchedBy == null)
            return UNKNOWN;

        switch (matchedBy) {
            case ALL_SUPPLIED:
                final var pncInRequest = Optional.ofNullable(result.getMatchRequest())
                        .map(MatchRequest::getPncNumber)
                        .isPresent();
                if (pncInRequest) return NAME_DOB_PNC;
                else return NAME_DOB;
            case ALL_SUPPLIED_ALIAS:
                return NAME_DOB_ALIAS;
            case NAME:
                return NAME;
            case PARTIAL_NAME:
                return PARTIAL_NAME;
            case PARTIAL_NAME_DOB_LENIENT:
                return PARTIAL_NAME_DOB_LENIENT;
            case EXTERNAL_KEY:
                return EXTERNAL_KEY;
            case NOTHING:
                return NOTHING;
            default:
                log.warn("Unknown OffenderSearchMatchType received {}", matchedBy.name());
                return UNKNOWN;
        }
    }
}
