package uk.gov.justice.probation.courtcasematcher.model.courtcaseservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantTypeTest {

    @ParameterizedTest(name = "{0} is ORGANISATION TYPE")
    @DisplayName("Organisation defendant type")
    @ValueSource(strings = {"O", "o"})
    void givenOrganisationDefendantType_whenMatch_ThenReturn(String defendantType) {
        assertThat(DefendantType.of(defendantType)).isSameAs(DefendantType.ORGANISATION);
    }

    @ParameterizedTest(name = "{0} is PERSON TYPE")
    @DisplayName("Person defendant type")
    @ValueSource(strings = {"P", "p", "", "X"})
    void givenPersonDefendantType_whenMatch_ThenReturnPerson(String defendantType) {
        assertThat(DefendantType.of(defendantType)).isSameAs(DefendantType.PERSON);
    }

    @DisplayName("null defendant type passed")
    @Test
    void givenPersonDefendantType_whenMatchOnNull_ThenReturnPerson() {
        assertThat(DefendantType.of(null)).isSameAs(DefendantType.PERSON);
    }
}
