package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

public enum MatchType {
    NAME_DOB;

    public static MatchType of(OffenderSearchMatchType offenderSearchMatchType) {
        switch (offenderSearchMatchType) {
            case ALL_SUPPLIED:
                return NAME_DOB;
            default:
                return null;
        }
    }
}
