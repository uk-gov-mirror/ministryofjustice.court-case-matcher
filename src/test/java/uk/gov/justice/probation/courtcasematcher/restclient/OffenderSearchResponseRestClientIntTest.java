package uk.gov.justice.probation.courtcasematcher.restclient;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;

import java.time.LocalDate;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OffenderSearchResponseRestClientIntTest {

    @Autowired
    private OffenderSearchRestClient restClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .port(8090)
            .stubRequestLoggingDisabled(false)
            .usingFilesUnderClasspath("mocks"));


    @Test
    public void givenSingleMatchReturned_whenSearch_thenReturnIt() {
        Optional<SearchResponse> match = restClient.search("Arthur MORGAN", LocalDate.of(1975, 1, 1))
                .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(MatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(1);
        Offender offender = match.get().getMatches().get(0).getOffender();
        assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
        assertThat(offender.getOtherIds().getCro()).isEqualTo("1234ABC");
        assertThat(offender.getOtherIds().getPnc()).isEqualTo("ABCD1234");
    }

    @Test
    public void givenMultipleMatchesReturned_whenSearch_thenReturnThem() {
        Optional<SearchResponse> match = restClient.search("John MARSTON", LocalDate.of(1982, 4, 5))
                .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(MatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(2);

        Offender offender1 = match.get().getMatches().get(0).getOffender();
        assertThat(offender1.getOtherIds().getCrn()).isEqualTo("Y346123");
        assertThat(offender1.getOtherIds().getCro()).isEqualTo("2234DEF");
        assertThat(offender1.getOtherIds().getPnc()).isEqualTo("BBCD1567");

        Offender offender2 = match.get().getMatches().get(1).getOffender();
        assertThat(offender2.getOtherIds().getCrn()).isEqualTo("Z346124");
        assertThat(offender2.getOtherIds().getCro()).isEqualTo("3234DEG");
        assertThat(offender2.getOtherIds().getPnc()).isEqualTo("CBCD1568");
    }
}