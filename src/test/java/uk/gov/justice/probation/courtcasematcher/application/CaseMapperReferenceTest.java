package uk.gov.justice.probation.courtcasematcher.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CaseMapperReferenceTest {

    private static CaseMapperReference caseMapperReference;

    @BeforeAll
    static void beforeAll() {
        caseMapperReference = new CaseMapperReference();
        caseMapperReference.setDefaultProbationStatus("No record");
        // We get rid of spaces in the key because that's what happens with spring properties loading
    }

    @DisplayName("Get default probation status for unknown value")
    @Test
    void getDefaultStatus() {
        assertThat(caseMapperReference.getDefaultProbationStatus()).isEqualTo("No record");
    }

}
