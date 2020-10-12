package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MatchTypeTest {

    @Test
    @DisplayName("Direct equivalent match type")
    void givenSameMatchType_whenMatch_ThenReturn() {
        assertThat(MatchType.of(OffenderSearchMatchType.NAME)).isSameAs(MatchType.NAME);
        assertThat(MatchType.of(OffenderSearchMatchType.PARTIAL_NAME)).isSameAs(MatchType.PARTIAL_NAME);
        assertThat(MatchType.of(OffenderSearchMatchType.PARTIAL_NAME_DOB_LENIENT)).isSameAs(MatchType.PARTIAL_NAME_DOB_LENIENT);
        assertThat(MatchType.of(OffenderSearchMatchType.NOTHING)).isSameAs(MatchType.NOTHING);
    }

    @Test
    @DisplayName("Mapping all supplied variations to their equivalent")
    void toUpperCase_ShouldGenerateTheExpectedUppercaseValue() {
        assertThat(MatchType.of(OffenderSearchMatchType.ALL_SUPPLIED)).isSameAs(MatchType.NAME_DOB);
        assertThat(MatchType.of(OffenderSearchMatchType.ALL_SUPPLIED_ALIAS)).isSameAs(MatchType.NAME_DOB_ALIAS);
    }

}
