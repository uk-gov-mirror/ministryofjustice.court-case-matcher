package uk.gov.justice.probation.courtcasematcher.restclient;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.justice.probation.courtcasematcher.application.TestMessagingConfig;
import uk.gov.justice.probation.courtcasematcher.model.externaldocumentrequest.Name;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.MatchRequest;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.Offender;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.OffenderSearchMatchType;
import uk.gov.justice.probation.courtcasematcher.model.offendersearch.SearchResponse;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockExtension;
import uk.gov.justice.probation.courtcasematcher.wiremock.WiremockMockServer;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestMessagingConfig.class)
public class OffenderSearchRestClientIntTest {

    @Autowired
    private OffenderSearchRestClient restClient;

    private final MatchRequest.Factory matchRequestFactory = new MatchRequest.Factory();

    private static final WiremockMockServer MOCK_SERVER = new WiremockMockServer(8090);

    @RegisterExtension
    static WiremockExtension wiremockExtension = new WiremockExtension(MOCK_SERVER);

    @Test
    public void givenSingleMatchReturned_whenSearch_thenReturnIt() {
        var name = Name.builder().forename1("Arthur").surname("MORGAN").build();
        var match = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1975, 1, 1))).blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(1);
        assertThat(match.get().isExactMatch()).isTrue();

        var offender = match.get().getMatches().get(0).getOffender();
        assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
        assertThat(offender.getOtherIds().getCroNumber()).isEqualTo("1234ABC");
        assertThat(offender.getOtherIds().getPncNumber()).isEqualTo("ABCD1234");
        assertThat(offender.getProbationStatus().getStatus()).isEqualTo("CURRENT");
        assertThat(offender.getProbationStatus().getInBreach()).isTrue();
        assertThat(offender.getProbationStatus().isPreSentenceActivity()).isFalse();
        assertThat(offender.getProbationStatus().getPreviouslyKnownTerminationDate()).isEqualTo(LocalDate.of(2020, Month.FEBRUARY, 2));
    }

    @Test
    public void givenSingleMatchReturned_whenSearchWithPncNoDob_thenReturnIt() {
        var name = Name.builder().forename1("Arthur").surname("MORGAN").build();
        var match = restClient.search(matchRequestFactory.buildFrom("2004/0012345U", name, LocalDate.of(1975, 1, 1))).blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(1);
        assertThat(match.get().isExactMatch()).isTrue();

        var offender = match.get().getMatches().get(0).getOffender();
        assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
        assertThat(offender.getOtherIds().getCroNumber()).isEqualTo("1234ABC");
        assertThat(offender.getOtherIds().getPncNumber()).isEqualTo("2004/0012345U");
    }

    @Test
    public void givenSingleMatchReturned_whenSearchWithPncWithDob_thenReturnIt() {
        var name = Name.builder().forename1("Arthur").surname("MORGAN").build();
        matchRequestFactory.setUseDobWithPnc(true);
        var match = restClient.search(matchRequestFactory.buildFrom("2004/0012345U", name, LocalDate.of(1975, 1, 1))).blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(1);
        assertThat(match.get().isExactMatch()).isTrue();

        var offender = match.get().getMatches().get(0).getOffender();
        assertThat(offender.getOtherIds().getCrn()).isEqualTo("X346204");
        assertThat(offender.getOtherIds().getCroNumber()).isEqualTo("1234ABC");
        assertThat(offender.getOtherIds().getPncNumber()).isEqualTo("2004/0012345U");
    }

    @Test
    public void givenSingleMatchReturned_whenSearch_thenVerifyMono() {
        Name name = Name.builder().forename1("Arthur").surname("MORGAN").build();
        Mono<SearchResponse> matchMono = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1975, 1, 1)));

        StepVerifier.create(matchMono)
            .consumeNextWith(match -> Assertions.assertAll(
                () -> assertThat(match.getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED),
                () -> assertThat(match.getMatches().size()).isEqualTo(1)
            ))
            .verifyComplete();
    }

    @Test
    public void givenSingleMatchNonExactMatchReturned_whenSearch_thenReturnIt() {
        Name name = Name.builder().forename1("Calvin").surname("HARRIS").build();
        Optional<SearchResponse> match = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1969, Month.AUGUST, 26)))
            .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.NAME);
        assertThat(match.get().getMatches().size()).isEqualTo(1);
        assertThat(match.get().isExactMatch()).isFalse();
    }

    @Test
    public void givenMultipleMatchesReturned_whenSearch_thenReturnThem() {
        Name name = Name.builder().forename1("John").surname("MARSTON").build();
        Optional<SearchResponse> match = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)))
                .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.ALL_SUPPLIED);
        assertThat(match.get().getMatches().size()).isEqualTo(2);

        Offender offender1 = match.get().getMatches().get(0).getOffender();
        assertThat(offender1.getOtherIds().getCrn()).isEqualTo("Y346123");
        assertThat(offender1.getOtherIds().getCroNumber()).isEqualTo("2234DEF");
        assertThat(offender1.getOtherIds().getPncNumber()).isEqualTo("BBCD1567");

        Offender offender2 = match.get().getMatches().get(1).getOffender();
        assertThat(offender2.getOtherIds().getCrn()).isEqualTo("Z346124");
        assertThat(offender2.getOtherIds().getCroNumber()).isEqualTo("3234DEG");
        assertThat(offender2.getOtherIds().getPncNumber()).isEqualTo("CBCD1568");
    }

    @Test
    public void givenNoMatchesReturned_whenSearch_thenReturnEmptyList() {
        Name name = Name.builder().forename1("Juan").surname("MARSTONEZ").build();
        Optional<SearchResponse> match = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)))
                .blockOptional();

        assertThat(match).isPresent();
        assertThat(match.get().getMatchedBy()).isEqualTo(OffenderSearchMatchType.NOTHING);
        assertThat(match.get().getMatches().size()).isEqualTo(0);
    }

    @Test
    public void givenUnexpected500_whenSearch_thenRetryAndError() {
        Name name = Name.builder().forename1("error").surname("error").build();
        Mono<SearchResponse> searchResponseMono = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)));

        StepVerifier.create(searchResponseMono)
            .expectError(reactor.core.Exceptions.retryExhausted("Retries exhausted: 2/2", null).getClass())
            .verify();
    }

    @Test
    public void givenUnexpected404_whenSearch_thenNoRetryButReturnSameError() {
        Name name = Name.builder().forename1("not").surname("found").build();
        Mono<SearchResponse> searchResponseMono = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1999, 4, 5)));

        StepVerifier.create(searchResponseMono)
            .expectError(WebClientResponseException.NotFound.class)
            .verify();
    }

    @Test
    public void givenUnexpected401_whenSearch_thenNoRetryButReturnSameError() {
        Name name = Name.builder().forename1("unauthorised").surname("unauthorised").build();
        Mono<SearchResponse> searchResponseMono = restClient.search(matchRequestFactory.buildFrom(null, name, LocalDate.of(1982, 4, 5)));

        StepVerifier.create(searchResponseMono)
            .expectError(WebClientResponseException.Unauthorized.class)
            .verify();
    }

}
