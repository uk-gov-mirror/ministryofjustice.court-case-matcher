package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CourtCaseTest {

    @Test
    void givenOrg_whenShouldMatch_thenReturnFalse() {
        CourtCase courtCase = CourtCase.builder().defendantType(DefendantType.ORGANISATION).build();
        assertThat(courtCase.shouldMatchToOffender()).isFalse();
    }

    @Test
    void givenMatchedPerson_whenShouldMatch_thenReturnFalse() {
        CourtCase courtCase = CourtCase.builder().defendantType(DefendantType.PERSON).crn("X321567").build();
        assertThat(courtCase.shouldMatchToOffender()).isFalse();
    }

    @Test
    void givenSpacesOnlyInCrnPerson_whenShouldMatch_thenReturnTrue() {
        CourtCase courtCase = CourtCase.builder().defendantType(DefendantType.PERSON).crn("   ").build();
        assertThat(courtCase.shouldMatchToOffender()).isTrue();
    }

    @Test
    void givenUnMatchedPerson_whenShouldMatch_thenReturnTrue() {
        CourtCase courtCase = CourtCase.builder().defendantType(DefendantType.PERSON).build();
        assertThat(courtCase.shouldMatchToOffender()).isTrue();
    }
}
