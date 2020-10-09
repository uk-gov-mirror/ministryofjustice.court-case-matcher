package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum DefendantType {
    ORGANISATION("O"),
    PERSON("P")
    ;

    private static final DefendantType DEFAULT = PERSON;

    final String type;

    DefendantType(String type) {
        this.type = type;
    }

    public static DefendantType of(String defType) {
        defType = defType == null ? DEFAULT.name() : defType.toUpperCase();
        switch (defType) {
            case "O":
                return ORGANISATION;
            case "P":
                return PERSON;
            default:
                log.warn("Unknown defendant type received {}. Returning PERSON.", defType);
                return DEFAULT;
        }
    }
}
