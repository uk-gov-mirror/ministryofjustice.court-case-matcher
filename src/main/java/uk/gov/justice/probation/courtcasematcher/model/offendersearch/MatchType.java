package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum MatchType {
    NAME_DOB,
    NAME,
    PARTIAL_NAME,
    PARTIAL_NAME_DOB_LENIENT,
    NOTHING,
    UNKNOWN;

    public static MatchType of(OffenderSearchMatchType offenderSearchMatchType) {
        switch (offenderSearchMatchType) {
            case ALL_SUPPLIED:
                return NAME_DOB;
            case NAME:
                return NAME;
            case PARTIAL_NAME:
                return PARTIAL_NAME;
            case PARTIAL_NAME_DOB_LENIENT:
                return PARTIAL_NAME_DOB_LENIENT;
            case NOTHING:
                return NOTHING;
            default:
                log.warn("Unknown OffenderSearchMatchType received {}", offenderSearchMatchType.name());
                return UNKNOWN;
        }
    }
}
