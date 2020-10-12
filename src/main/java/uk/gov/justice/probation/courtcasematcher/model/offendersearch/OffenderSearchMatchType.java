package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

public enum OffenderSearchMatchType {
    ALL_SUPPLIED,
    /** Matches to all the parameters supplied but at least one from the offender's alias */
    ALL_SUPPLIED_ALIAS,
    HMPPS_KEY,
    EXTERNAL_KEY,
    NAME,
    PARTIAL_NAME,
    PARTIAL_NAME_DOB_LENIENT,
    NOTHING

}
