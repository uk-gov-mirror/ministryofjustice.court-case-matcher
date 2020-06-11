package uk.gov.justice.probation.courtcasematcher.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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
        caseMapperReference.setCourtNameToCodes(Map.of("SheffieldMagistratesCourt", "SHF", "DodgeCity", "DGC"));
    }

    @DisplayName("Get default probation status for unknown value")
    @Test
    void getDefaultStatus() {
        assertThat(caseMapperReference.getDefaultProbationStatus()).isEqualTo("No record");
    }

    @DisplayName("Get court code for name")
    @Test
    void getCourtCodeForName() {
        assertThat(caseMapperReference.getCourtCodeFromName("Sheffield Magistrates Court")).get().isEqualTo("SHF");
    }

    @DisplayName("Get court code for unknown name")
    @Test
    void getCourtCodeForUnknownName() {
        assertThat(caseMapperReference.getCourtCodeFromName("Transylvania")).isEmpty();
    }
}
