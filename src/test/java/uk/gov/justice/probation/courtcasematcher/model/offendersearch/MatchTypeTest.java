package uk.gov.justice.probation.courtcasematcher.model.offendersearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.probation.courtcasematcher.service.SearchResult;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MatchTypeTest {

    @Mock
    private MatchRequest.Factory factory;

    @Test
    @DisplayName("Direct equivalent match type")
    void givenSameMatchType_whenMatch_ThenReturn() {
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.NAME))).isSameAs(MatchType.NAME);
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.PARTIAL_NAME))).isSameAs(MatchType.PARTIAL_NAME);
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.PARTIAL_NAME_DOB_LENIENT))).isSameAs(MatchType.PARTIAL_NAME_DOB_LENIENT);
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.NOTHING))).isSameAs(MatchType.NOTHING);
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.EXTERNAL_KEY))).isSameAs(MatchType.EXTERNAL_KEY);
    }

    @Test
    @DisplayName("Mapping all supplied variations to their equivalent")
    void toUpperCase_ShouldGenerateTheExpectedUppercaseValue() {
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.ALL_SUPPLIED, MatchRequest.builder()
                .pncNumber(null)
                .build()
        ))).isSameAs(MatchType.NAME_DOB);
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.ALL_SUPPLIED, MatchRequest.builder()
                .pncNumber("PNC")
                .build()
        ))).isSameAs(MatchType.NAME_DOB_PNC);
        assertThat(MatchType.of(buildSearchResult(OffenderSearchMatchType.ALL_SUPPLIED_ALIAS))).isSameAs(MatchType.NAME_DOB_ALIAS);
    }

    private SearchResult buildSearchResult(OffenderSearchMatchType name) {
        return buildSearchResult(name, MatchRequest.builder()
                .pncNumber("something")
                .build());
    }

    private SearchResult buildSearchResult(OffenderSearchMatchType name, MatchRequest matchRequest) {
        return SearchResult.builder()
                .searchResponse(SearchResponse.builder()
                        .matchedBy(name)
                        .build())
                .matchRequest(matchRequest)
                .build();
    }

}
